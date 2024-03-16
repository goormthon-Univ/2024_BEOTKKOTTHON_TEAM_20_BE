package com.beotkkot.qtudy.service.posts;

import com.beotkkot.qtudy.domain.posts.Posts;
import com.beotkkot.qtudy.domain.scrap.Scrap;
import com.beotkkot.qtudy.domain.tags.Tags;
import com.beotkkot.qtudy.domain.user.Users;
import com.beotkkot.qtudy.dto.object.PostListItem;
import com.beotkkot.qtudy.dto.request.posts.PostsRequestDto;
import com.beotkkot.qtudy.dto.response.*;
import com.beotkkot.qtudy.repository.posts.PostsRepository;
import com.beotkkot.qtudy.repository.scrap.ScrapRepository;
import com.beotkkot.qtudy.repository.tags.TagsRepository;
import com.beotkkot.qtudy.repository.user.UserRepository;
import com.beotkkot.qtudy.dto.response.posts.GetPostsAllResponseDto;
import com.beotkkot.qtudy.dto.response.posts.GetPostsResponseDto;
import com.beotkkot.qtudy.dto.response.posts.PostsResponseDto;
import com.beotkkot.qtudy.dto.response.posts.PutScrapResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PostsService {
    private final PostsRepository postsRepo;
    private final UserRepository userRepo;
    private final TagsRepository tagRepo;
    private final ScrapRepository scrapRepo;
//    private final CommentsRepository commentsRepo; (댓글 기능 구현 시 주석 제거)

    @Transactional
    public ResponseEntity<? super PostsResponseDto> savePost(Long kakao_uid, PostsRequestDto dto) {
        try {

            if (userRepo.findByKakaoId(kakao_uid) != null) {
                // 포스트 엔티티 생성
                Posts post = dto.toEntity(kakao_uid);

                // 태그 처리
                List<String> postTags = dto.getTag();
                List<String> savedTags = new ArrayList<>();

                for (String tagName : postTags) {
                    Optional<Tags> existingTag = tagRepo.findByName(tagName);
                    if (existingTag.isPresent()) {
                        // 기존에 있는 태그인 경우 count를 증가시킴
                        Tags tag = existingTag.get();
                        tag.increaseTagCount();
                        savedTags.add(tagName);
                    } else {
                        // 새로운 태그인 경우 태그를 생성하고 count를 1로 초기화함
                        Tags newTag = new Tags();
                        newTag.setName(tagName);
                        newTag.setCount(1); // 새로운 태그의 count를 1로 초기화
                        savedTags.add(tagName);

                        // 새로운 태그를 저장
                        tagRepo.save(newTag);
                    }
                }

                // 저장된 태그 목록을 포스트에 설정
                String tagString = String.join(",", savedTags);
                post.setTag(tagString);

                // 포스트 저장
                postsRepo.save(post);
            } else {
                return PostsResponseDto.notExistUser();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }
        return PostsResponseDto.success();
    }

    @Transactional
    public ResponseEntity<? super GetPostsResponseDto> getPost(Long postId) {
        Posts post;
        Users user;
        try {
            if (postsRepo.existsById(postId)) {
                post = postsRepo.findById(postId).get();
                user = userRepo.findByKakaoId(post.getUserUid());
            } else {
                return GetPostsResponseDto.noExistPost();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }

        return GetPostsResponseDto.success(post, user);
    }

    @Transactional
    public ResponseEntity<? super GetPostsAllResponseDto> getAllPost() {
        List<PostListItem> postListItems = new ArrayList<>();
        try {
            List<Posts> posts = postsRepo.findAll();
            for (Posts post : posts)
                postListItems.add(PostListItem.of(post));
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }

        GetPostsAllResponseDto responseDto = new GetPostsAllResponseDto(postListItems);
        return responseDto.success(postListItems);
    }

    @Transactional
    public ResponseEntity<? super PostsResponseDto> patchPost(Long postId, Long kakao_uid, PostsRequestDto dto) {
        try {
            Optional<Posts> postOptional = postsRepo.findById(postId);
            if (!postOptional.isPresent()) return PostsResponseDto.notExistedPost();

            Posts post = postOptional.get();
            if (userRepo.findByKakaoId(kakao_uid) == null) return PostsResponseDto.notExistUser();

            Long writerId = post.getUserUid();
            if (!writerId.equals(kakao_uid)) return PostsResponseDto.noPermission();

            // 업데이트되기 이전의 태그 목록
            List<String> existingTags = Arrays.asList(post.getTag().split(","));

            // 업데이트된 태그 목록
            List<String> updatedTags = dto.getTag();

            // 태그 카운트를 증감시키기 위한 로직
            for (String tagName : existingTags) {
                if (!updatedTags.contains(tagName)) {
                    // 태그가 삭제된 경우 카운트 감소
                    Optional<Tags> tagOptional = tagRepo.findByName(tagName);
                    tagOptional.ifPresent(Tags::decreaseTagCount);
                }
            }
            for (String tagName : updatedTags) {
                if (!existingTags.contains(tagName)) {
                    Optional<Tags> existTag = tagRepo.findByName(tagName);
                    if (existTag.isPresent()) {
                        // 기존에 있는 태그인 경우 count를 증가시킴
                        Tags tag = existTag.get();
                        tag.increaseTagCount();
                    } else {
                        // 새로운 태그인 경우 태그를 생성하고 count를 1로 초기화함
                        Tags newTag = new Tags();
                        newTag.setName(tagName);
                        newTag.setCount(1); // 새로운 태그의 count를 1로 초기화

                        // 새로운 태그를 저장
                        tagRepo.save(newTag);
                    }
                }
            }

            post.patchPost(dto);
            postsRepo.save(post);

        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }
        return PostsResponseDto.success();
    }

    @Transactional
    public ResponseEntity<? super PostsResponseDto> deletePost(Long postId, Long kakao_uid) {
        Posts post = postsRepo.findById(postId).get();
        try{
            if (!postsRepo.existsById(postId)) return PostsResponseDto.notExistedPost();
            if (userRepo.findByKakaoId(kakao_uid) == null) return PostsResponseDto.notExistUser();

            Long writerId = post.getUserUid();
            boolean isWriter = writerId.equals(kakao_uid);
            if (!isWriter) return PostsResponseDto.noPermission();

            // ********* comment, scrap 함께 삭제 (댓글 기능 구현 시 수정 바람)
            scrapRepo.deleteByPostId(postId);
            // commentRepo.deleteByPostId(postId);

            // 관련된 hash tag -1
            List<String> tagNameList = Arrays.asList(post.getTag().split("\\s*,\\s*"));
            List<Tags> tagList = tagRepo.findByNames(tagNameList);
            for (Tags tag: tagList)
                tag.decreaseTagCount();

            postsRepo.delete(post);

        } catch(Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }

        return PostsResponseDto.success();
    }

    @Transactional
    public ResponseEntity<? super GetPostsAllResponseDto> getMyPost(Long kakao_uid) {
        List<PostListItem> postListItems = new ArrayList<>();;
        try {
            List<Posts> posts = postsRepo.findAllByUserUid(kakao_uid);
            for (Posts post : posts)
                postListItems.add(PostListItem.of(post));
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }

        GetPostsAllResponseDto responseDto = new GetPostsAllResponseDto(postListItems);
        return responseDto.success(postListItems);
    }

    @Transactional
    public ResponseEntity<? super GetPostsAllResponseDto> getSearchPost(String searchWord) {
        List<PostListItem> postListItems = new ArrayList<>();
        try {
            List<Posts> posts = postsRepo.findBySearchWord(searchWord);
            for (Posts post : posts)
                postListItems.add(PostListItem.of(post));
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }

        GetPostsAllResponseDto responseDto = new GetPostsAllResponseDto(postListItems);
        return responseDto.success(postListItems);
    }

    @Transactional
    public ResponseEntity<? super GetPostsAllResponseDto> getCategorySearchPost(List<Long> categories) {
        List<PostListItem> postListItems = new ArrayList<>();
        try {
            for (Long category : categories) {
                List<Posts> posts = postsRepo.findByCategoryId(category);

                for (Posts post : posts) {
                    postListItems.add(PostListItem.of(post));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }

        GetPostsAllResponseDto responseDto = new GetPostsAllResponseDto(postListItems);
        return responseDto.success(postListItems);
    }

    // 스크랩
    @Transactional
    public ResponseEntity<? super PutScrapResponseDto> putScrap(Long postId, Long kakao_uid) {
        try {
            if (userRepo.findByKakaoId(kakao_uid) == null) return PutScrapResponseDto.notExistUser();
            Posts post = postsRepo.findByPostId(postId);
            if (post == null) return PutScrapResponseDto.notExistedPost();

            Scrap scrap = scrapRepo.findByPostIdAndUserId(postId, kakao_uid);

            // 존재하지 않는다면 추가. 존재한다면 삭제
            if (scrap == null) {
                scrap = new Scrap(kakao_uid, postId);
                scrapRepo.save(scrap);
                post.increaseScrapCount();
            }
            else {
                scrapRepo.delete(scrap);
                post.decreaseScrapCount();
            }

            postsRepo.save(post);

        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }
        return PutScrapResponseDto.success();
    }

    @Transactional
    public ResponseEntity<? super GetPostsAllResponseDto> getAllScrapPost(Long kakao_uid) {
        List<PostListItem> postListItems = new ArrayList<>();
        try {
            if (userRepo.findByKakaoId(kakao_uid) == null) return PutScrapResponseDto.notExistUser();

            List<Long> postIdList = scrapRepo.findPostIdsByUserId(kakao_uid);
            List<Posts> posts = postsRepo.findByPostIds(postIdList);
            if (posts == null) return PutScrapResponseDto.notExistedPost();

            for (Posts post : posts)
                postListItems.add(PostListItem.of(post));
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }

        GetPostsAllResponseDto responseDto = new GetPostsAllResponseDto(postListItems);
        return responseDto.success(postListItems);
    }
}