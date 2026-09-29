package sg.edu.nus.foc.order.application;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import sg.edu.nus.foc.order.domain.OrderProblem;

class ResultWrapperTest {
    @Test
    void successAndFailureAreExclusiveAndMappingDoesNotRunOnFailure() {
        ResultWrapper<String> success = ResultWrapper.success("saved");
        assertThat(success.isError()).isFalse();
        assertThat(success.getError()).isNull();
        assertThat(success.map(String::length).getData()).isEqualTo(5);
        OrderProblem problem = OrderProblem.conflict("Already accepted");
        ResultWrapper<String> failure = ResultWrapper.failure(problem);
        assertThat(failure.getData()).isNull();
        assertThat(
                        failure.map(
                                        value -> {
                                            throw new AssertionError("Must not map failures");
                                        })
                                .getError())
                .isSameAs(problem);
        assertThatThrownBy(() -> ResultWrapper.success(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ResultWrapper.failure(null))
                .isInstanceOf(NullPointerException.class);
    }
}
