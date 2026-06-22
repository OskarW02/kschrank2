package de.htwberlin.dbtech.aufgaben.ue03;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.htwberlin.dbtech.exceptions.DataException;

public class TrayGateway {

    private final Connection connection;

    public TrayGateway(Connection connection) {
        this.connection = connection;
    }

    public boolean existsDiameter(Integer diameterInCM) {

        String sql = "SELECT COUNT(trayid) AS ANZAHL FROM tray WHERE diameterincm = ?";

        try (PreparedStatement pStmt = connection.prepareStatement(sql)) {

            pStmt.setInt(1, diameterInCM);

            try (ResultSet rs = pStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ANZAHL") > 0;
                }
                return false;
            }

        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    public List<Integer> findCandidateTrays(Integer diameterInCM, LocalDate sampleExpirationDate) {

        String sql =
                "SELECT trayid " +
                        "FROM tray " +
                        "WHERE diameterincm = ? " +
                        "AND expirationdate > ? " +
                        "ORDER BY expirationdate";

        List<Integer> result = new ArrayList<>();

        try (PreparedStatement pStmt = connection.prepareStatement(sql)) {

            pStmt.setInt(1, diameterInCM);
            pStmt.setDate(2, java.sql.Date.valueOf(sampleExpirationDate));

            try (ResultSet rs = pStmt.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getInt("trayid"));
                }
            }

            return result;

        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    public Integer findEmptyTray(Integer diameterInCM) {

        String sql =
                "SELECT t.trayid " +
                        "FROM tray t " +
                        "LEFT JOIN place p ON t.trayid = p.trayid " +
                        "WHERE t.diameterincm = ? " +
                        "GROUP BY t.trayid " +
                        "HAVING COUNT(p.sampleid) = 0 " +
                        "ORDER BY t.trayid";

        try (PreparedStatement pStmt = connection.prepareStatement(sql)) {

            pStmt.setInt(1, diameterInCM);

            try (ResultSet rs = pStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("trayid");
                }
                return null;
            }

        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    public void updateExpirationDate(Integer trayId, LocalDate expirationDate) {

        String sql = "UPDATE tray SET expirationdate = ? WHERE trayid = ?";

        try (PreparedStatement pStmt = connection.prepareStatement(sql)) {

            pStmt.setDate(1, java.sql.Date.valueOf(expirationDate));
            pStmt.setInt(2, trayId);

            pStmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataException(e);
        }
    }
}