package com.example.withdogandcat.domain.hashtag.chattag;

import com.example.withdogandcat.domain.chat.dto.ChatRoomDto;
import com.example.withdogandcat.domain.chat.entity.ChatRoomEntity;
import com.example.withdogandcat.domain.chat.repo.ChatRoomJpaRepository;
import com.example.withdogandcat.domain.chat.util.ChatRoomMapper;
import com.example.withdogandcat.domain.pet.PetRepository;
import com.example.withdogandcat.domain.pet.dto.PetResponseDto;
import com.example.withdogandcat.domain.pet.entity.Pet;
import com.example.withdogandcat.domain.user.entity.User;
import com.example.withdogandcat.global.exception.BaseException;
import com.example.withdogandcat.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ChatRoomTagService {

    private final PetRepository petRepository;
    private final ChatRoomJpaRepository chatRoomRepository;
    private final ChatRoomTagRepository chatRoomTagRepository;
    private final ChatRoomTagMapRepository chatRoomTagMapRepository;

    private final int MAX_TAGS_PER_ROOM = 2;

    /**
     * 태그 등록
     */
    @Transactional
    public List<ChatRoomTagDto> addTagToChatRoom(String roomId, List<String> tags, Long userId) {
        return tags.stream().map(tagName -> {
            if (tagName == null || tagName.trim().isEmpty()) {
                throw new BaseException(BaseResponseStatus.ELEMENTS_IS_REQUIRED);
            }

            ChatRoomEntity chatRoom = chatRoomRepository.findByRoomId(roomId)
                    .orElseThrow(() -> new BaseException(BaseResponseStatus.CHATROOM_NOT_FOUND));

            if (!chatRoom.getCreatorId().getUserId().equals(userId)) {
                throw new BaseException(BaseResponseStatus.ACCESS_DENIED);
            }

            long currentTagCount = chatRoomTagMapRepository.countByChatRoom(chatRoom);
            if (currentTagCount >= MAX_TAGS_PER_ROOM) {
                throw new BaseException(BaseResponseStatus.EXCEED_MAX_TAG_LIMIT);
            }

            ChatRoomTag tag = chatRoomTagRepository.findByName(tagName)
                    .orElseGet(() -> chatRoomTagRepository.save(new ChatRoomTag(null, tagName)));

            if (chatRoomTagMapRepository.findByChatRoomAndChatRoomTag(chatRoom, tag).isPresent()) {
                throw new BaseException(BaseResponseStatus.ALREADY_EXISTS);
            }

            ChatRoomTagMap chatRoomTagMap = ChatRoomTagMap.builder()
                    .chatRoom(chatRoom)
                    .chatRoomTag(tag)
                    .build();
            chatRoomTagMapRepository.save(chatRoomTagMap);

            return ChatRoomTagDto.from(tag);
        }).collect(Collectors.toList());
    }

    /**
     * 특정 태그 등록한 모든 채팅방 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getChatRoomsByTag(String tagName) {
        ChatRoomTag tag = chatRoomTagRepository.findByName(tagName)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.TAG_NOT_FOUND));

        return chatRoomTagMapRepository.findByChatRoomTag(tag).stream()
                .map(chatRoomTagMap -> {
                    ChatRoomEntity chatRoom = chatRoomTagMap.getChatRoom();
                    User creator = chatRoom.getCreatorId();
                    List<Pet> pets = petRepository.findByUser(creator);
                    List<PetResponseDto> petDtos = pets.stream()
                            .map(PetResponseDto::from)
                            .collect(Collectors.toList());

                    List<ChatRoomTagDto> chatRoomTags = chatRoomTagMapRepository.findByChatRoom(chatRoom).stream()
                            .map(ChatRoomTagMap::getChatRoomTag)
                            .map(ChatRoomTagDto::from)
                            .collect(Collectors.toList());

                    return ChatRoomMapper.toDtoWithTags(chatRoom, petDtos, chatRoomTags);
                }).collect(Collectors.toList());
    }


    /**
     * 태그 삭제
     */
    @Transactional
    public void removeTagFromChatRoom(String roomId, String tagName, Long requesterUserId) {
        ChatRoomEntity chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.CHATROOM_NOT_FOUND));

        ChatRoomTag tag = chatRoomTagRepository.findByName(tagName)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.TAG_NOT_FOUND));

        ChatRoomTagMap chatRoomTagMap = chatRoomTagMapRepository.findByChatRoomAndChatRoomTag(chatRoom, tag)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.CHATROOM_TAG_NOT_FOUND));

        if (!chatRoom.getCreatorId().getUserId().equals(requesterUserId)) {
            throw new BaseException(BaseResponseStatus.ACCESS_DENIED);
        }

        chatRoomTagMapRepository.delete(chatRoomTagMap);

        long count = chatRoomTagMapRepository.countByChatRoomTag(tag);
        if (count == 0) {
            chatRoomTagRepository.delete(tag);
        }
    }

    /**
     * 모든 태그 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomTagDto> getAllTags() {
        List<ChatRoomTag> allTags = chatRoomTagRepository.findAll();
        return allTags.stream()
                .map(ChatRoomTagDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 인기 태그 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomTagDto> getPopularChatRoomTags(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> tagFrequencies = chatRoomTagMapRepository.findChatRoomTagUsageFrequency(pageable);
        return tagFrequencies.stream()
                .map(obj -> new ChatRoomTagDto((Long) obj[0], (String) obj[1]))
                .collect(Collectors.toList());
    }

    /**
     * 특정 채팅방의 모든 태그 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomTagDto> getTagsForChatRoom(String roomId) {
        ChatRoomEntity chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.CHATROOM_NOT_FOUND));

        return chatRoomTagMapRepository.findByChatRoom(chatRoom).stream()
                .map(chatRoomTagMap -> ChatRoomTagDto.from(chatRoomTagMap.getChatRoomTag()))
                .collect(Collectors.toList());
    }

}
