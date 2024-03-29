package com.example.airplanning.controller.api;


import com.example.airplanning.configuration.login.UserDetail;
import com.example.airplanning.domain.Response;
import com.example.airplanning.domain.dto.board.BoardDto;
import com.example.airplanning.domain.dto.board.*;
import com.example.airplanning.domain.enum_class.Category;
import com.example.airplanning.domain.enum_class.LikeType;
import com.example.airplanning.exception.AppException;
import com.example.airplanning.exception.ErrorCode;
import com.example.airplanning.service.BoardService;
import com.example.airplanning.service.LikeService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
@Slf4j
public class BoardRestController {

    private final BoardService boardService;
    private final LikeService likeService;

    @GetMapping("/{category}/list")
    @ApiOperation(value = "게시판 리스트 조회", notes = "게시판 리스트를 조회합니다. 누구나 조회 가능하며, 제목과 작성자로 검색 할 수 있습니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "category", value = "게시판 카테고리 - FREE, RANK_UP, REPORT, PORTFOLIO 중 하나를 선택", defaultValue = "free"),
            @ApiImplicitParam(name = "searchType", value = "검색 조건 - 제목, 작성자 중 하나를 입력, REPORT 게시판은 검색 안됨", defaultValue = "제목"),
            @ApiImplicitParam(name = "keyword", value = "검색어", defaultValue = "None")})
    public Response<?> listBoard(@ApiIgnore @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                 @PathVariable Category category,
                                 @RequestParam(required = false) String searchType,
                                 @RequestParam(required = false) String keyword) {

        if (searchType != null) {
            if (category.equals(Category.REPORT)) searchType = null;
            else if (searchType.equals("제목")) searchType = "TITLE";
            else if (searchType.equals("작성자")) searchType = "NICKNAME";
            else searchType = null;
        }

        Page<BoardListResponse> boardPage;

        if (category.equals(Category.PORTFOLIO)) {
            boardPage = boardService.portfolioList(pageable, searchType, keyword, null, 998L);
            return Response.success(boardPage);
        } else {
            boardPage = boardService.boardList(pageable, searchType, keyword, category);
            return Response.success(boardPage);
        }
    }

    @PostMapping("/{category}")
    @ApiOperation(value = "게시판 글 작성", notes = "게시판에 글을 작성합니다.<br/>자유게시판 글 등록은 로그인한 유저만 가능합니다.<br/>플래너 신청은 일반 유저만 가능하고 regionId(자신있는 지역), amount(플랜 가격)도 작성해야 합니다.<br/>포트폴리오 등록은 플래너만 가능합니다.<br/>신고 게시판 글 등록에서는 title에 신고 대상의 닉네임을 적어야 합니다.")
    @ApiImplicitParam(name = "category", value = "게시판 카테고리 - free, rankup, portfolio, report 중 하나를 입력", defaultValue = "free")
    public Response<?> writeBoard(@PathVariable String category, @ApiIgnore Principal principal,
                                  @RequestPart(value = "request") BoardCreateRequest req,
                                  @RequestPart(value = "file",required = false) MultipartFile file) {

        category = category.toLowerCase();
        Category enumCategory;
        if (category.equals("free")) enumCategory = Category.FREE;
        else if (category.equals("rankup")) enumCategory = Category.RANK_UP;
        else if (category.equals("portfolio")) enumCategory = Category.PORTFOLIO;
        else if (category.equals("report")) enumCategory = Category.REPORT;
        else throw new AppException(ErrorCode.INVALID_REQUEST);

        try {
            return Response.success(boardService.writeWithFile(req, file, principal.getName(), enumCategory));
        } catch (AppException e) {
            if (e.getErrorCode().equals(ErrorCode.FILE_UPLOAD_ERROR)) { //S3 업로드 오류
                return Response.error("파일 업로드 과정 중 오류가 발생했습니다. 다시 시도해 주세요.");
            } else {
                throw e;
            }
        } catch (Exception e) {
            return Response.error("에러 발생");
        }
    }

    @GetMapping("/{boardId}")
    @ApiOperation(value = "게시판 글 조회", notes = "게시판 글 하나를 조회합니다. 자유게시판과 포트폴리오 게시판의 글에는 누구나 조회 가능하지만, 신고 게시판과 플래너 신청 게시판은 작성자와 ADMIN만 조회 가능합니다.")
    @ApiImplicitParam(name = "boardId", value = "게시판 글 Id")
    public Response<?> detailBoard(@PathVariable Long boardId,
                                   @ApiIgnore Principal principal,
                                   @ApiIgnore @AuthenticationPrincipal UserDetail userDetail){

        Category category = boardService.findCategory(boardId);
        BoardDto boardDto = boardService.detail(boardId, false, category);

        if (category.equals(Category.REPORT) || category.equals(Category.RANK_UP)) {
            // 작성자 본인이거나 ADMIN이 아니면 에러 발생
            if (!principal.getName().equals(boardDto.getUserName()) && !userDetail.getRole().equals("ADMIN")) {
                throw new AppException(ErrorCode.INVALID_PERMISSION);
            }
        }

        return Response.success(boardDto);
    }

    @PutMapping("/{boardId}")
    @ApiOperation(value = "게시판 글 수정", notes = "게시판 글을 수정합니다.<br/>본인이 작성한 글만 수정 가능합니다.<br/>플래너 신청 수정 시, regionId(자신있는 지역), amount(플랜 가격)도 작성해야 합니다.<br/>신고 게시판 글 수정에서는 title에 신고 대상의 닉네임을 적어야 합니다.")
    @ApiImplicitParam(name = "boardId", value = "게시판 Id", defaultValue = "None")
    public Response<?> updateBoard(@PathVariable Long boardId,
                                   @RequestPart(value = "request") BoardUpdateRequest req,
                                   @RequestPart(value = "file",required = false) MultipartFile file,
                                   @ApiIgnore Principal principal) throws IOException {

        Category category = boardService.findCategory(boardId);

        try {
            return Response.success(boardService.modify(req, file, principal.getName(), boardId, category));
        } catch (AppException e) {
            if (e.getErrorCode().equals(ErrorCode.FILE_UPLOAD_ERROR)) { //S3 업로드 오류
                return Response.error("파일 업로드 과정 중 오류가 발생했습니다. 다시 시도해 주세요.");
            } else if (e.getErrorCode().equals(ErrorCode.BOARD_NOT_FOUND)) {
                return Response.error("게시글이 존재하지 않습니다.");
            } else if (e.getErrorCode().equals(ErrorCode.INVALID_PERMISSION)) { //작성자 수정자 불일치 (혹시 버튼이 아닌 url로 접근시 제한)
                return Response.error("작성자만 수정이 가능합니다.");
            } else {
                throw e;
            }
        } catch (Exception e) {
            return Response.error("에러 발생");
        }
    }

    @DeleteMapping("/{boardId}")
    @ApiOperation(value = "게시판 글 삭제", notes = "게시판 글 하나를 삭제합니다. 작성자와 ADMIN만 삭제 가능합니다.")
    public Response<String> delete(@PathVariable Long boardId, @ApiIgnore Principal principal) {
        try {
            boardService.delete(principal.getName(), boardId);
        } catch (Exception e) {
            return Response.error("글 삭제 중 에러가 발생하였습니다. 다시 시도해주세요.");
        }
        return Response.success("글이 삭제 되었습니다.");
    }

    @PostMapping("/{boardId}/like")
    @ApiOperation(value = "게시판 글 좋아요", notes = "게시판 글에 좋아요를 추가하거나 취소합니다.<br/>자유게시판 글만 가능하고 로그인한 유저만 가능합니다.")
    @ApiImplicitParam(name = "boardId", value = "게시판 글 Id")
    public String changeLike(@PathVariable Long boardId, @ApiIgnore Principal principal) {
        if (!boardService.findCategory(boardId).equals(Category.FREE)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return likeService.changeLike(boardId, principal.getName(), LikeType.BOARD_LIKE);
    }

}

