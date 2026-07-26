package com.example.starter.domain.port.out;

import com.example.starter.domain.model.Position;
import java.util.List;

/** Outbound port: read every position still open, for bulk processing. */
public interface LoadOpenPositionsPort {

    List<Position> findOpen();
}
