package com.example.withdogandcat.domain.mypage;


import com.example.withdogandcat.domain.chat.dto.ChatRoomListDto;
import com.example.withdogandcat.domain.chat.entity.ChatRoomEntity;
import com.example.withdogandcat.domain.chat.repo.ChatRoomJpaRepository;
import com.example.withdogandcat.domain.chat.service.ChatMessageService;
import com.example.withdogandcat.domain.chat.util.ChatRoomMapper;
import com.example.withdogandcat.domain.hashtag.chattag.ChatRoomTagDto;
import com.example.withdogandcat.domain.hashtag.chattag.ChatRoomTagService;
import com.example.withdogandcat.domain.pet.PetRepository;
import com.example.withdogandcat.domain.pet.dto.PetResponseDto;
import com.example.withdogandcat.domain.pet.entity.Pet;
import com.example.withdogandcat.domain.review.ReviewRepository;
import com.example.withdogandcat.domain.shop.repo.ShopRepository;
import com.example.withdogandcat.domain.shop.dto.ShopResponseDto;
import com.example.withdogandcat.domain.shop.entity.Shop;
import com.example.withdogandcat.domain.user.UserRepository;
import com.example.withdogandcat.domain.user.entity.User;
import com.example.withdogandcat.global.common.BaseResponse;
import com.example.withdogandcat.global.exception.BaseException;
import com.example.withdogandcat.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final PetRepository petRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ChatRoomTagService chatRoomTagService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomJpaRepository chatRoomJpaRepository;

    /**
     * 등록 가게 조회
     */
    @Transactional(readOnly = true)
    public BaseResponse<List<ShopResponseDto>> getShopsByCurrentUser(User currentUser) {
        List<Shop> shops = shopRepository.findByUser(currentUser);
        if (shops.isEmpty()) {
            return new BaseResponse<>(BaseResponseStatus.SHOP_NOT_FOUND);
        }

        List<ShopResponseDto> shopDtos = shops.stream()
                .map(shop -> {
                    int reviewCount = reviewRepository.countByShop(shop);
                    return ShopResponseDto.from(shop, reviewCount);
                }).collect(Collectors.toList());
        return new BaseResponse<>(BaseResponseStatus.SUCCESS, "성공", shopDtos);
    }

    /**
     * 등록 반려동물 조회
     */
    @Transactional(readOnly = true)
    public BaseResponse<List<PetResponseDto>> getUserPets(User currentUser) {
        List<Pet> pets = petRepository.findByUser(currentUser);
        List<PetResponseDto> petDtos = pets.stream()
                .map(PetResponseDto::from).collect(Collectors.toList());
        return new BaseResponse<>(BaseResponseStatus.SUCCESS, "성공", petDtos);
    }

    /**
     * 등록 채팅방 조회 + 태그
     */
    @Transactional(readOnly = true)
    public BaseResponse<List<ChatRoomListDto>> findRoomsCreatedByUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

        List<ChatRoomEntity> userRooms = chatRoomJpaRepository.findByCreatorId(user);
        List<ChatRoomListDto> chatRoomListDtos = userRooms.stream()
                .map(room -> {
                    List<ChatRoomTagDto> tags = chatRoomTagService.getTagsForChatRoom(room.getRoomId());
                    return ChatRoomMapper.toChatRoomListDto(
                            room, chatMessageService.getLastTalkMessage(room.getRoomId()), tags, petRepository);
                }).collect(Collectors.toList());

        return new BaseResponse<>(BaseResponseStatus.SUCCESS, "사용자가 생성한 채팅방 목록 조회 성공", chatRoomListDtos);
    }

}
