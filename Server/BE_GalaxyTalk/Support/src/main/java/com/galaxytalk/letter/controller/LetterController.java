package com.galaxytalk.letter.controller;

import com.galaxytalk.letter.dto.ApiResponseDto;
import com.galaxytalk.letter.dto.EnergyRequest;
import com.galaxytalk.letter.dto.Letter;
import com.galaxytalk.letter.dto.LetterIdRequest;
import com.galaxytalk.letter.feign.AuthClient;
import com.galaxytalk.letter.service.LetterService;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/letter")
public class LetterController {

    private final LetterService letterService;
    private final AuthClient authClient;

    public LetterController(LetterService letterService, AuthClient authClient) {
        this.letterService = letterService;
        this.authClient = authClient;
    }


    //후기 쓰기
    //user로 가서 에너지 1 늘려주기
    @PostMapping
    public ResponseEntity<?> writeLetter(@RequestBody Letter letter) {

        try {
            //1. 받는 사람이 존재하는지 확인
            authClient.getUserInfo(letter.getReceiverId());
            //2. 주는 사람이 존재하는지 확인
            authClient.getUserInfo(letter.getSenderId());

        } catch (FeignException ex) {
            String errorMessage = "서버와의 연결에 문제가 발생했습니다.";
            if (ex.status() == 400) {
                // 400 BadRequest 시 추가적인 메시지 처리
                errorMessage = "유저가 확인되지 않습니다.";
            }
            return new ResponseEntity<>(new ApiResponseDto(false, errorMessage, null), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            // 다른 예외 처리
            return new ResponseEntity<>(new ApiResponseDto(false, "알 수 없는 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        letterService.saveLetter(letter);


        ApiResponseDto successResponse = new ApiResponseDto(true, "편지 저장 성공", null);

        authClient.increaseEnergy(new EnergyRequest(letter.getSenderId(), letter.getReceiverId()));

        return ResponseEntity.ok(successResponse);

    }

    //내게 남겨진 후기 목록 보기
    @GetMapping
    public ResponseEntity<?> getLetters(@RequestHeader("X-User-ID") String serialNumber) {

        try {
            //1. 받는 사람이 존재하는지 확인
            authClient.getUserInfo(serialNumber);

        } catch (FeignException ex) {
            String errorMessage = "서버와의 연결에 문제가 발생했습니다.";
            if (ex.status() == 400) {
                // 400 BadRequest 시 추가적인 메시지 처리
                errorMessage = "유저가 확인되지 않습니다.";
            }
            return new ResponseEntity<>(new ApiResponseDto(false, errorMessage, null), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            // 다른 예외 처리
            return new ResponseEntity<>(new ApiResponseDto(false, "알 수 없는 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<Letter> letterList = letterService.getLetters(serialNumber);


        if (letterList.size() == 0 || letterList.isEmpty())
            return ResponseEntity.ok(new ApiResponseDto(true, "편지가 비었어요", null));

        ApiResponseDto successResponse = new ApiResponseDto(true, "편지 불러오기 성공", letterList);
        return ResponseEntity.ok(successResponse);
    }

    //편지 hide true로 바꾸기
    @PutMapping("/hide")
    @Transactional
    public ResponseEntity<?> hideLetter(@RequestBody LetterIdRequest letterId) {

        System.out.println("들어오긴하고있나??");
        Letter letter = letterService.getAletter(letterId.getLetterId());


        if (letter == null)
            return new ResponseEntity<>(new ApiResponseDto(false, "편지 검색 안됨", null), HttpStatus.BAD_REQUEST);

        letter.setIsHide(1);
        ApiResponseDto successResponse = new ApiResponseDto(true, "편지 숨기기 성공", null);
        return ResponseEntity.ok(successResponse);
    }


    //내게 남겨진 후기 하나 보기
    @GetMapping("/{letterId}")
    public ResponseEntity<?> getLetter(@PathVariable Long letterId) {

        Letter letter = letterService.getAletter(letterId);


        if (letter == null)
            return ResponseEntity.ok(new ApiResponseDto(true, "편지가 비었어요", null));

        ApiResponseDto successResponse = new ApiResponseDto(true, "편지 불러오기 성공", letter);
        return ResponseEntity.ok(successResponse);
    }

    //채팅방에 따라 내가 작성한 후기 보기
    @GetMapping("/chat/{chatRoomId}")
    public ResponseEntity<?> getLetter(@RequestHeader("X-User-ID") String serialNumber, @PathVariable String chatRoomId) {

        Letter letter = letterService.getChatletter(chatRoomId, serialNumber);


        if (letter == null)
            return ResponseEntity.ok(new ApiResponseDto(true, "편지가 비었어요", null));

        ApiResponseDto successResponse = new ApiResponseDto(true, "편지 불러오기 성공", letter);
        return ResponseEntity.ok(successResponse);


    }

}