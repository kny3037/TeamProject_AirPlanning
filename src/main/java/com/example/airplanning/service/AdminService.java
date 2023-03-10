package com.example.airplanning.service;

import com.example.airplanning.configuration.login.UserDetail;
import com.example.airplanning.domain.dto.board.RankUpRequest;
import com.example.airplanning.domain.dto.user.UserDto;
import com.example.airplanning.domain.entity.Board;
import com.example.airplanning.domain.entity.Planner;
import com.example.airplanning.domain.entity.Region;
import com.example.airplanning.domain.entity.User;
import com.example.airplanning.domain.enum_class.AlarmType;
import com.example.airplanning.domain.enum_class.UserRole;
import com.example.airplanning.exception.AppException;
import com.example.airplanning.exception.ErrorCode;
import com.example.airplanning.repository.BoardRepository;
import com.example.airplanning.repository.PlannerRepository;
import com.example.airplanning.repository.RegionRepository;
import com.example.airplanning.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final SessionRegistry sessionRegistry;

    private final PlannerRepository plannerRepository;
    private final RegionRepository regionRepository;

    private final AlarmService alarmService;

    private final BoardRepository boardRepository;

    @Transactional
    public UserDto changeRankToPlanner(RankUpRequest request) {
        User user = userRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUNDED));

        user.changeRank("PLANNER");
        User changedUser = userRepository.save(user);

        String region1 = request.getRegion().split(" ")[0];
        String region2 = request.getRegion().split(" ")[1];
        Region selectedRegion = regionRepository.findByRegion1AndRegion2(region1, region2)
                        .orElseThrow(() -> new AppException(ErrorCode.REGION_NOT_FOUND));


        plannerRepository.save(request.toEntity(changedUser, selectedRegion));

        alarmService.send(user, AlarmType.CHANGE_ROLE_ALARM, "/users/mypage", "PLANNER");

        Board board = boardRepository.findById(request.getBoardId())
                .orElseThrow(()->new AppException(ErrorCode.BOARD_NOT_FOUND));

        boardRepository.delete(board);

        return UserDto.of(changedUser);
    }

    @Transactional
    public UserDto changeRank(String nickname, String role, Long boardId) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUNDED));

        user.changeRank(role);
        User changedUser = userRepository.save(user);

        Optional<Planner> planner = plannerRepository.findByUser(changedUser);
        if (planner.isPresent()) {
                plannerRepository.delete(planner.get());
        }

        alarmService.send(user, AlarmType.CHANGE_ROLE_ALARM, "/users/mypage", role);

        Board board = boardRepository.findById(boardId)
                .orElseThrow(()->new AppException(ErrorCode.BOARD_NOT_FOUND));

        boardRepository.delete(board);

        if (role.equals(UserRole.BLACKLIST.name())) {
            List<UserDetail> userDetails = sessionRegistry.getAllPrincipals()
                    .stream().map(o ->(UserDetail) o).collect(Collectors.toList());

            for (UserDetail userDetail : userDetails) {
                if (userDetail.getId() == user.getId()) {
                    List<SessionInformation> sessionList = sessionRegistry.getAllSessions(userDetail, false);
                    for (SessionInformation session : sessionList) {
                        session.expireNow();
                    }
                }
            }
        }
        return UserDto.of(changedUser);
    }
}
