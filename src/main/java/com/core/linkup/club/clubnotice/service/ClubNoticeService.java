package com.core.linkup.club.clubnotice.service;

import com.core.linkup.club.club.entity.Club;
import com.core.linkup.club.club.repository.ClubMemberRepository;
import com.core.linkup.club.club.repository.ClubRepository;
import com.core.linkup.club.clubnotice.converter.ClubNoticeConverter;
import com.core.linkup.club.clubnotice.entity.ClubNotice;
import com.core.linkup.club.clubnotice.entity.enums.NotificationType;
import com.core.linkup.club.clubnotice.repository.ClubNoticeRepository;
import com.core.linkup.club.clubnotice.request.ClubNoticeRequest;
import com.core.linkup.club.clubnotice.response.ClubCommentResponse;
import com.core.linkup.club.clubnotice.response.ClubNoticeResponse;
import com.core.linkup.common.exception.BaseException;
import com.core.linkup.common.response.BaseResponseStatus;
import com.core.linkup.member.entity.Member;
import com.core.linkup.member.repository.MemberRepository;
import com.core.linkup.security.MemberDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClubNoticeService {

    private final ClubRepository clubRepository;
    private final ClubNoticeRepository clubNoticeRepository;
    private final ClubNoticeConverter clubNoticeConverter;
    private final ClubCommentService clubCommentService;
    private final MemberRepository memberRepository;
    private final ClubMemberRepository clubMemberRepository;

    // 소모임 공지 작성
    public ClubNoticeResponse createNotice(MemberDetails member, Long clubId, ClubNoticeRequest request) {
        Long memberId = member.getId();
        checkClubId(clubId);

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.INVALID_CLUB_ID));

        if (request.type().equals(NotificationType.NOTICE)) {
            checkClubOwner(club, memberId);

            ClubNotice clubNotice = clubNoticeConverter.toClubNoticeEntity(request, clubId, memberId);
            clubNoticeRepository.save(clubNotice);

            return clubNoticeConverter.toClubNoticeResponse(clubNotice, member.getMember());
        } else {
            // 소모임 가입 인원인지 체크
            checkClubMember(club, memberId);

            ClubNotice clubNotice = clubNoticeConverter.toClubNoticeEntity(request, clubId, memberId);
            clubNoticeRepository.save(clubNotice);

            return clubNoticeConverter.toClubNoticeResponse(clubNotice, member.getMember());
        }
    }

    //소모임 공지 전체 조회
    public Page<ClubNoticeResponse> findAllNotice(Long clubId, MemberDetails memberDetails, Pageable pageable) {

        Page<ClubNotice> clubNotices = clubNoticeRepository.findAllNotice(clubId, pageable);
        List<Member> members = memberRepository.findAllById(clubNotices.stream()
                .map(ClubNotice::getMemberId)
                .collect(Collectors.toList()));

        Map<Long, Member> memberMap = members.stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));

        List<ClubNoticeResponse> clubNoticeResponseList = clubNotices.stream()
                .map(clubNotice -> {
                    Member member = memberMap.get(clubNotice.getMemberId());
                    return clubNoticeConverter.toClubNoticeResponse(clubNotice, member);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(clubNoticeResponseList, pageable, clubNotices.getTotalElements());
    }

    // 소모임 게시판 전체 조회
    public Page<ClubNoticeResponse> findAllBoard(Long clubId, MemberDetails memberDetails, Pageable pageable) {

        Page<ClubNotice> clubNotices = clubNoticeRepository.findAllBoard(clubId, pageable);
        List<Member> members = memberRepository.findAllById(clubNotices.stream()
                .map(ClubNotice::getMemberId)
                .collect(Collectors.toList()));

        Map<Long, Member> memberMap = members.stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));

        List<ClubNoticeResponse> clubBoardResponseList = clubNotices.stream()
                .map(clubNotice -> {
                    Member member = memberMap.get(clubNotice.getMemberId());
                    return clubNoticeConverter.toClubNoticeResponse(clubNotice, member);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(clubBoardResponseList, pageable, clubNotices.getTotalElements());
    }

    //소모임 공지/게시글 개별 조회
    public ClubNoticeResponse findNotice(Member member, Long clubId, Long noticeId) {
        checkClubId(clubId);
        ClubNotice notice = clubNoticeRepository.findNotice(clubId, noticeId);
        Member writer = memberRepository.findById(notice.getMemberId()).orElseThrow(
                () -> new BaseException(BaseResponseStatus.INVALID_WRITER));

        List<ClubCommentResponse> comments = clubCommentService.findComments(member, clubId, noticeId);

        return clubNoticeConverter.toClubNoticeResponse(notice, comments, writer);
    }

    //소모임 수정
    public ClubNoticeResponse updateNotice(MemberDetails member, Long clubId, Long noticeId, ClubNoticeRequest request) {
        Long memberId = member.getId();
        Club club = checkClubId(clubId);

        ClubNotice clubNotice = clubNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.INVALID_NOTICE));

        // 공지사항 수정 시
        if (clubNotice.getType().equals(NotificationType.NOTICE)){
            checkClubOwner(club, memberId);
            ClubNotice updatedNotice = clubNoticeConverter.toUpdateNoticeEntity(clubNotice, request, memberId);
            ClubNotice save = clubNoticeRepository.save(updatedNotice);

            return clubNoticeConverter.toClubNoticeResponse(save, member.getMember());
        } else {
            // 게시글 수정 시, 작성자인지 체크
            if (!clubNotice.getMemberId().equals(memberId)){
                throw new BaseException(BaseResponseStatus.INVALID_CLUB_MEMBER);
            }
            ClubNotice updatedNotice = clubNoticeConverter.toUpdateNoticeEntity(clubNotice, request, memberId);
            ClubNotice save = clubNoticeRepository.save(updatedNotice);

            return clubNoticeConverter.toClubNoticeResponse(save, member.getMember());
        }
    }

    //삭제
    public void deleteNotice(MemberDetails member, Long clubId, Long noticeId) {
        Long memberId = member.getId();
        Club club = checkClubId(clubId);

        // 공지사항 존재 여부 확인
        ClubNotice clubNotice = clubNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.INVALID_NOTICE));

        if (clubNotice.getType().equals(NotificationType.NOTICE)){
            checkClubOwner(club, memberId);
            clubNoticeRepository.delete(clubNotice);
        } else {
            // 게시글 작성자인지 체크 후 삭제
            if (!clubNotice.getMemberId().equals(memberId)){
                throw new BaseException(BaseResponseStatus.INVALID_WRITER);
            }
            clubNoticeRepository.delete(clubNotice);
        }
    }

    private void checkClubMember(Club club, Long memberId) {
        boolean isWriter = clubMemberRepository.existsByClubIdAndMemberId(club.getId(), memberId);
        boolean isOwner = club.getMemberId().equals(memberId);
        if (!isWriter && !isOwner){
            throw new BaseException(BaseResponseStatus.INVALID_CLUB_MEMBER);
        }
    }

    private Club checkClubId(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.INVALID_CLUB_ID));
        return club;
    }

    private static void checkClubOwner(Club club, Long memberId) {
        if (!club.getMemberId().equals(memberId)) {
            throw new BaseException(BaseResponseStatus.INVALID_CLUB_OWNER);
        }
    }
}
