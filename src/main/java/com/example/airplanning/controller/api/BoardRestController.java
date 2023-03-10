package com.example.airplanning.controller.api;


import com.example.airplanning.domain.Response;
import com.example.airplanning.domain.dto.board.BoardDto;
import com.example.airplanning.domain.dto.board.*;
import com.example.airplanning.domain.entity.Board;
import com.example.airplanning.domain.enum_class.Category;
import com.example.airplanning.exception.AppException;
import com.example.airplanning.exception.ErrorCode;
import com.example.airplanning.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
@Slf4j
public class BoardRestController {

    private final BoardService boardService;

    // 삭제
    @DeleteMapping("/{boardId}")
    public Response<String> delete(@PathVariable Long boardId, Principal principal) {

        boardService.delete(principal.getName(), boardId);
        return Response.success("글 삭제가 완료되었습니다.");
    }


    // 플래너 신청 등록
    @PostMapping("/rankUpWrite/{boardId}")
    public Response<BoardResponse>rankUpWrite(@RequestBody BoardCreateRequest boardCreateRequest, Principal principal) {
        String userName = principal.getName();
        BoardDto boardDto = boardService.write(boardCreateRequest, "test");
        return Response.success(new BoardResponse("프래너 등급 신청이 완료되었습니다.", boardDto.getId()));
    }

    // 플래너 신청 조회
    @GetMapping("/rankUp/{boardId}")
    @Operation(summary = "플래너 신청 조회")
    public Response<RankUpDetailResponse> rankUpDetail(@PathVariable Long boardId){
        return Response.success( boardService.rankUpDetail(boardId));
    }

    // 유저 신고 작성
    @PostMapping("/reportWrite/{boardId}")
    public Response<BoardResponse>reportWrite(@RequestBody ReportCreateRequest reportCreateRequest, Principal principal) throws IOException {
        String userName = principal.getName();
        Board board = boardService.reportWrite(reportCreateRequest, null, userName);
        return Response.success(new BoardResponse("신고가 완료되었습니다.", board.getId()));
    }
    
    
    // 유저 신고 상세 조회
    @GetMapping("/reportDetail/{boardId}")
    public Response<BoardDto> reportDetail(@PathVariable Long boardId) {
        BoardDto boardDto =  boardService.reportDetail(boardId);
        return Response.success(boardDto);
    }


    // 유저 신고 수정
    @PutMapping("/reportModify/{boardId}")
    public Response<BoardResponse> reportModify(@PathVariable Long boardId, @RequestBody ReportModifyRequest reportModifyRequest, Principal principal) throws IOException {
        String userName = principal.getName();
        BoardDto boardDto = boardService.reportModify(reportModifyRequest, null, userName, boardId);
        return Response.success(new BoardResponse("신고 수정이 완료되었습니다.", boardDto.getId()));
    }


    // 유저 신고 삭제
    @DeleteMapping("/reportDelete/{boardId}")
    public Response<BoardResponse> reportDelete(@PathVariable Long boardId, Principal principal) {
        String userName = principal.getName();
        Long deleteReport = boardService.reportDelete("test", boardId);
        return Response.success(new BoardResponse("신고 삭제가 완료되었습니다.", boardId));
    }


    // 유저 신고 리스트
    @GetMapping
    public Response<Page<BoardListResponse>> reportList(@PageableDefault(sort = "createdAt",size = 20,direction = Sort.Direction.DESC) Pageable pageable){
        Page<BoardListResponse> boardDto = boardService.reportList(pageable);
        return Response.success(boardDto);
    }

}

