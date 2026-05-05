package com.cadac.stone_inscription.report.moderation;

public abstract class ModerationHandler {

    protected ModerationHandler next;

    public ModerationHandler setNext(ModerationHandler next) {
        this.next = next;
        return next;
    }

    public abstract void handle(ModerationExecutionContext context);

    protected void passToNext(ModerationExecutionContext context) {
        if (next != null) {
            next.handle(context);
        }
    }
}
