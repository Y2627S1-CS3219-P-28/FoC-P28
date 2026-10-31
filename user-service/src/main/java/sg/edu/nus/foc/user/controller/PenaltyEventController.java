package sg.edu.nus.foc.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import sg.edu.nus.foc.user.event.PenaltyEventConsumer;
import sg.edu.nus.foc.user.dto.PubSubPushRequest;

@RestController
@RequestMapping("/api/users/internal/penalties")
@RequiredArgsConstructor
public class PenaltyEventController {

    private final PenaltyEventConsumer penaltyEventConsumer;

    // POST /api/users/internal/penalties/addition
    @PostMapping("/addition")
    public ResponseEntity<Void> receiveAddition(
            @RequestBody PubSubPushRequest request
    ) {

        penaltyEventConsumer.consumeAddition(request);

        return ResponseEntity.ok().build();
    }

    // POST /api/users/internal/penalties/deduction
    @PostMapping("/deduction")
    public ResponseEntity<Void> receiveDeduction(
            @RequestBody PubSubPushRequest request
    ) {

        penaltyEventConsumer.consumeDeduction(request);

        return ResponseEntity.ok().build();
    }
}