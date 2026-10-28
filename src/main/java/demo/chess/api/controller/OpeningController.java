package demo.chess.api.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.OpeningDto;
import demo.chess.api.dto.OpeningUpdateDto;
import demo.chess.api.service.BundledOpeningCatalogService;
import demo.chess.api.service.OpeningLibraryService;
import demo.chess.definitions.engines.impl.NoMoveFoundException;

/** REST boundary for the reusable opening library. */
@RestController
@RequestMapping("/api/openings")
public class OpeningController {

    private final OpeningLibraryService openingLibraryService;
    private final BundledOpeningCatalogService bundledOpeningCatalogService;

    public OpeningController(
            OpeningLibraryService openingLibraryService,
            BundledOpeningCatalogService bundledOpeningCatalogService) {
        this.openingLibraryService = openingLibraryService;
        this.bundledOpeningCatalogService = bundledOpeningCatalogService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OpeningDto>> getOpenings() {
        return ResponseEntity.ok(openingLibraryService.getOpenings());
    }

    @PostMapping(
            value = "/import",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importOpenings(
            @RequestBody String content,
            @RequestParam(name = "fileName", required = false) String fileName) {
        try {
            return ResponseEntity.ok(openingLibraryService.importPgn(content, fileName));
        } catch (NoMoveFoundException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("I/O error while importing openings");
        }
    }

    @PostMapping(
            value = "/catalogs/lichess/import",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importLichessCatalog() {
        try {
            return ResponseEntity.ok(bundledOpeningCatalogService.importLichessCatalog());
        } catch (NoMoveFoundException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("I/O error while importing bundled Lichess openings");
        }
    }

    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> renameOpening(
            @PathVariable("id") String id,
            @RequestBody OpeningUpdateDto update) {
        try {
            return ResponseEntity.ok(openingLibraryService.renameOpening(
                    id,
                    update == null ? null : update.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOpening(@PathVariable("id") String id) {
        try {
            openingLibraryService.deleteOpening(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
