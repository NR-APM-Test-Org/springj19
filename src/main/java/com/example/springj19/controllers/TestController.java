package com.example.springj19.controllers;

import jdk.incubator.concurrent.StructuredTaskScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@RestController
@RequestMapping(value = "/test")
public class TestController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello world";
    }

    @GetMapping("/list")
    public List<Integer> PreAllocatedList() {
        var preAllocated = new ArrayList<Integer>(44);
        preAllocated.add(5);
        return preAllocated;
    }

    @GetMapping("/matchVals")
    public Map<String, String> PatternMatch() {
        var map = new HashMap<String, String>(44);
        map.put("5greater", patternMatchResponse("greaterthan5"));
        map.put("str", patternMatchResponse("str"));
        map.put("int", patternMatchResponse(44));
        map.put("default", patternMatchResponse(true));
        return map;
    }

    @GetMapping("/recordPatternMatch")
    public Map<String, Double> recordPatternMatch() {
        var map = new HashMap<String, Double>(44);
        map.put("ifPosition", distanceFromOriginWithIf(new Position(3, 4)));
        map.put("else", distanceFromOriginWithIf(new Object()));
        map.put("positionSwitch", switchRecord(new Position(3, 4)));
        map.put("pathSwitch", switchRecord(new Path(new Position(3, 4), new Position(6, 8))));
        map.put("default", switchRecord(new Object()));
        return map;
    }

    @GetMapping("/structuredConcurrencySuccess")
    public Path structuredConcurrencySuccess() {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            Future<Position> p1Future = scope.fork(() -> waitAndGetPosition(3, 4));
            Future<Position> p2Future = scope.fork(() -> waitAndGetPosition(3, 4));
            scope.join();
            scope.throwIfFailed();
            return new Path(p1Future.get(), p2Future.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/structuredConcurrencyFail")
    public ResponseEntity<?> structuredConcurrencyFail() {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            Future<Position> p1Future = scope.fork(() -> waitAndGetPosition(3, 4));
            Future<Position> p2Future = scope.fork(this::waitAndThrowError);
            scope.join();
            scope.throwIfFailed();
            var path = new Path(p1Future.get(), p2Future.get());
            return new ResponseEntity<>(path, HttpStatus.OK);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Position waitAndGetPosition(int x, int y) {
        try {
            Thread.sleep(1000);
            return new Position(x, y);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Position waitAndThrowError() {
        try {
            Thread.sleep(1000);
            throw new CustomException("Expected error");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private double distanceFromOriginWithIf(Object object) {
        if (object instanceof Position(int x,int y)) {
            return pass(Math.sqrt(x * x + y * y));
        }
        return pass(-1);
    }

    private double switchRecord(Object object) {
        return switch (object) {
            case Position(int X, int Y) -> pass(Math.sqrt(X * X + Y * Y));
            case Path(Position(int X1, int Y1), Position(int X2, int Y2)) -> pass(X1 + Y1 + X2 + Y2);
            default -> pass(-1);
        };
    }

    private <T> T pass(T val) { return val; }

    private String patternMatchResponse(Object obj) {
        return switch (obj) {
            case String s when s.length() > 5 -> "string > 5";
            case String s -> "string";
            case Integer i -> "Integer";
            default -> "default";
        };
    }

    public class CustomException extends RuntimeException {
        public CustomException(String v) {
            super(v);
        }
    }

    public record Position(int x, int y) {}
    public record Path(Position p1, Position p2) {}

}
