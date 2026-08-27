package com.example.memo.service;

import com.example.memo.domain.Memo;
import com.example.memo.domain.User;
import com.example.memo.dto.MemoCreateRequest;
import com.example.memo.dto.MemoResponse;
import com.example.memo.dto.MemoUpdateRequest;
import com.example.memo.dto.PageResponse;
import com.example.memo.exception.MemoNotFoundException;
import com.example.memo.exception.UnauthorizedException;
import com.example.memo.repository.MemoRepository;
import com.example.memo.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemoService {

    private final MemoRepository memoRepository;
    private final UserRepository userRepository;

    public MemoService(MemoRepository memoRepository, UserRepository userRepository) {
        this.memoRepository = memoRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MemoResponse create(MemoCreateRequest request) {
        User user = getCurrentUser();
        Memo memo = new Memo(request.title(), request.content(), user);
        Memo saved = memoRepository.save(memo);
        return MemoResponse.from(saved);
    }

    public PageResponse<MemoResponse> findAll(String keyword, String author, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasAuthor = author != null && !author.isBlank();

        Page<Memo> memos;
        if (hasAuthor && hasKeyword) {
            memos = memoRepository.findByUser_UsernameAndTitleContainingIgnoreCase(author.trim(), keyword.trim(), pageable);
        } else if (hasAuthor) {
            memos = memoRepository.findByUser_Username(author.trim(), pageable);
        } else if (hasKeyword) {
            memos = memoRepository.findByTitleContainingIgnoreCase(keyword.trim(), pageable);
        } else {
            memos = memoRepository.findAll(pageable);
        }

        return PageResponse.from(memos.map(MemoResponse::from));
    }

    public MemoResponse findById(Long id) {
        return MemoResponse.from(getMemoOrThrow(id));
    }

    @Transactional
    public MemoResponse update(Long id, MemoUpdateRequest request) {
        Memo memo = getMemoOrThrow(id);
        checkOwnership(memo);
        memo.update(request.title(), request.content());
        return MemoResponse.from(memo);
    }

    @Transactional
    public void delete(Long id) {
        Memo memo = getMemoOrThrow(id);
        checkOwnership(memo);
        memoRepository.delete(memo);
    }

    private Memo getMemoOrThrow(Long id) {
        return memoRepository.findById(id)
                .orElseThrow(() -> new MemoNotFoundException(id));
    }

    /** SecurityContext 에서 현재 로그인한 사용자를 가져옵니다. */
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }

    /** 메모의 작성자와 현재 로그인한 사용자가 같은지 확인합니다. */
    private void checkOwnership(Memo memo) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!memo.getUser().getUsername().equals(currentUsername)) {
            throw new UnauthorizedException("본인의 메모만 수정/삭제할 수 있습니다.");
        }
    }
}
