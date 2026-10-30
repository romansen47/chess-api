package demo.chess.api.service;

import demo.chess.api.dto.EngineConfigStoreDto;

/** Persistence port for engine definitions, profiles and default assignments. */
interface EngineSettingsRepository {

    EngineConfigStoreDto load();

    void save(EngineConfigStoreDto store);
}
