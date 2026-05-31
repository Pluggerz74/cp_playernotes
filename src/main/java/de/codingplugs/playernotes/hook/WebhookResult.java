package de.codingplugs.playernotes.hook;

public record WebhookResult(boolean success, int statusCode, String errorMessage) {

    public static WebhookResult success(int statusCode) {
        return new WebhookResult(true, statusCode, "");
    }

    public static WebhookResult httpFailure(int statusCode, String responseBody) {
        return new WebhookResult(false, statusCode, responseBody == null ? "" : responseBody);
    }

    public static WebhookResult failure(int statusCode, String errorMessage) {
        return new WebhookResult(false, statusCode, errorMessage == null ? "" : errorMessage);
    }
}
