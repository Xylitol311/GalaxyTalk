package com.galaxytalk.letter.controller;

import com.galaxytalk.feign.AuthClient;
import com.galaxytalk.letter.dto.*;
import com.galaxytalk.letter.service.LetterService;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    //user로 가서 에너지 1 늘려주기, requestId는 요청하지 말기
    @PostMapping
    public ResponseEntity<?> writeLetter(@RequestHeader("X-User-ID") String serialNumber, @RequestBody LetterRequest letterreq) {

        try {
            authClient.getUserInfo(letterreq.getReceiverId());
        } catch (FeignException ex) {
            HttpStatus status = HttpStatus.BAD_REQUEST;
            String errorMessage = "서버와의 연결에 문제가 발생했습니다.";

            if (ex.status() == 400) {
                errorMessage = "유저가 확인되지 않습니다.";
            } else if (ex.status() == 404) {
                errorMessage = "받는 유저가 존재하지 않습니다.";
            } else if (ex.status() >= 500) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                errorMessage = "외부 서비스 오류가 발생했습니다.";
            }

            return new ResponseEntity<>(new ApiResponseDto(false, errorMessage, null), status);
        }



        letterService.saveLetter(serialNumber,letterreq);


        ApiResponseDto successResponse = new ApiResponseDto(true, "편지 저장 성공", null);

        authClient.increaseEnergy(new EnergyRequest(serialNumber, letterreq.getReceiverId()));

        return ResponseEntity.ok(successResponse);

    }

    //내게 남겨진 후기 목록 보기, hide=1는 주지말기
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
    public ResponseEntity<?> hideLetter(@RequestHeader("X-User-ID") String serialNumber, @RequestBody LetterIdRequest letterId) {

        if(!letterService.hideLetter(letterId.getLetterId()))
            return new ResponseEntity<>(new ApiResponseDto(false, "잘못된 사용자 접근", null), HttpStatus.BAD_REQUEST);


        ApiResponseDto successResponse = new ApiResponseDto(true, "편지 숨기기 성공", null);
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