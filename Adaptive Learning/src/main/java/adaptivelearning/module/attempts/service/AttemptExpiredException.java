package adaptivelearning.module.attempts.service;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;

public class AttemptExpiredException extends CommonException {
    public AttemptExpiredException() {
        super(ErrorCode.ATTEMPT_EXPIRED);
    }
}
