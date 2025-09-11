package safemate.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import safemate.service.AIService;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/train")
    public ResponseEntity<String> train(@RequestBody Map<String, Object> payload) {
        double[][] x = ((java.util.List<java.util.List<Number>>) payload.get("x"))
                .stream().map(l -> l.stream().mapToDouble(Number::doubleValue).toArray())
                .toArray(double[][]::new);

        double[] y = ((java.util.List<Number>) payload.get("y"))
                .stream().mapToDouble(Number::doubleValue).toArray();

        String[] featureNames = ((java.util.List<String>) payload.get("features")).toArray(new String[0]);

        aiService.train(x, y, featureNames);
        return ResponseEntity.ok("Modelo entrenado correctamente.");
    }

    @PostMapping("/predict")
    public ResponseEntity<Double> predict(@RequestBody Map<String, Object> payload) {
        double[] features = ((java.util.List<Number>) payload.get("features"))
                .stream().mapToDouble(Number::doubleValue).toArray();

        double result = aiService.predict(features);
        return ResponseEntity.ok(result);
    }
}
