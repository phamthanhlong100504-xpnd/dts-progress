package com.dts.progress.simulationperformance;

import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes=SimulationProgressController.class)
public class SimulationProgressAdvice {
    @ExceptionHandler({IllegalArgumentException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().header("Cache-Control","no-store").body(Map.of("success",false,"code","SIMULATION_PROGRESS_INVALID_REQUEST","message","Invalid license, bank or watermark"));}
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<?> unavailable(Exception e){return ResponseEntity.status(503).header("Cache-Control","no-store").body(Map.of("success",false,"code","SIMULATION_PROGRESS_UNAVAILABLE","message","Simulation projection or migration is not ready"));}
}
