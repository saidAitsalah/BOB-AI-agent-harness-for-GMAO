package com.borneflow.mbo;

import com.borneflow.exception.BorneflowException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Collection de {@link ChargingStationMbo}, équivalent du {@code MboSet} de Maximo.
 *
 * <p>Permet d'itérer sur un ensemble de bornes et de leur appliquer des opérations
 * groupées (sauvegarde batch, validation collective).</p>
 */
public class ChargingStationMboSet implements Iterable<ChargingStationMbo> {

    private final List<ChargingStationMbo> stations = new ArrayList<>();

    public void add(ChargingStationMbo station) {
        stations.add(station);
    }

    public ChargingStationMbo get(int index) {
        return stations.get(index);
    }

    public int size() {
        return stations.size();
    }

    public boolean isEmpty() {
        return stations.isEmpty();
    }

    public List<ChargingStationMbo> asList() {
        return Collections.unmodifiableList(stations);
    }

    /** Sauvegarde toutes les bornes de la collection (équivalent {@code MboSet.save()}). */
    public void saveAll() throws BorneflowException {
        for (var s : stations) {
            s.save();
        }
    }

    @Override
    public Iterator<ChargingStationMbo> iterator() {
        return stations.iterator();
    }
}
