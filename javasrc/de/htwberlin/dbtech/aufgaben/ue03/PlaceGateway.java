package de.htwberlin.dbtech.aufgaben.ue03;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.htwberlin.dbtech.exceptions.DataException;

public class PlaceGateway {

    private final Connection connection;

    public PlaceGateway(Connection connection) {
        this.connection = connection;
    }

    public boolean hasFreeCapacity(Integer trayId) {

        String sql =
                "SELECT t.capacity, COUNT(p.sampleid) AS occupied " +
                        "FROM tray t " +
                        "LEFT JOIN place p ON t.trayid = p.trayid " +
                        "WHERE t.trayid = ? " +
                        "GROUP BY t.capacity";

        try (PreparedStatement pStmt = connection.prepareStatement(sql)) {

            pStmt.setInt(1, trayId);

            try (ResultSet rs = pStmt.executeQuery()) {
                if (rs.next()) {
                    int capacity = rs.getInt("capacity");
                    int occupied = rs.getInt("occupied");
                    return occupied < capacity;
                }
                return false;
            }

        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    public Integer findFirstFreePlace(Integer trayId) {

        String sql = "SELECT placeno FROM place WHERE trayid = ? ORDER BY placeno";

        try (PreparedStatement pStmt = connection.prepareStatement(sql)) {

            pStmt.setInt(1, trayId);

            try (ResultSet rs = pStmt.executeQuery()) {

                int expectedPlaceNo = 1;

                while (rs.next()) {
                    int currentPlaceNo = rs.getInt("placeno");

                    if (currentPlaceNo != expectedPlaceNo) {
                        return expectedPlaceNo;
                    }

                    expectedPlaceNo++;
                }

                return expectedPlaceNo;
            }

        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    public void insertPlace(Integer trayId, Integer placeNo, Integer sampleId) {

        String sql = "INSERT INTO place (trayid, placeno, sampleid) VALUES (?, ?, ?)";

        try (PreparedStatement pStmt = connection.prepareStatement(sql)) {

            pStmt.setInt(1, trayId);
            pStmt.setInt(2, placeNo);
            pStmt.setInt(3, sampleId);

            pStmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataException(e);
        }
    }
}