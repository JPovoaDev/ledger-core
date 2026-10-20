package dev.joao.ledger;

import io.javalin.Javalin;

public final class App {
    public static void main(String[] args) {
        var app = Javalin.create(config ->
            config.routes.get("/health", ctx ->
                ctx.contentType("application/json").result("{\"status\":\"UP\"}")));
        app.start(7070);
    }
}