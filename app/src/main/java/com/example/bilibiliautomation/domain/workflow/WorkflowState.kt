package com.example.bilibiliautomation.domain.workflow

enum class WorkflowState {
    IDLE,
    VALIDATE_ACCOUNT,
    OPEN_TARGET,
    LOAD_VIDEOS,
    OPEN_VIDEO,
    LOAD_COMMENTS,
    PROCESS_COMMENT,
    OPEN_USER,
    CHECK_RELATIONSHIP,
    PERFORM_ACTION,
    RETURN_TO_COMMENTS,
    NEXT_COMMENT,
    NEXT_VIDEO,
    COOLDOWN,
    PAUSED,
    FINISHED,
    FAILED,
    ;

    val isTerminal: Boolean
        get() = this == FINISHED || this == FAILED || this == COOLDOWN || this == PAUSED

    val isSafeCheckpoint: Boolean
        get() = this != IDLE
}
