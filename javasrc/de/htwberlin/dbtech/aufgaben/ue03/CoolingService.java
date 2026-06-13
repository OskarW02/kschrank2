package de.htwberlin.dbtech.aufgaben.ue03;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import de.htwberlin.dbtech.exceptions.CoolingSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.htwberlin.dbtech.exceptions.DataException;

public class CoolingService implements ICoolingService {
    private static final Logger L = LoggerFactory.getLogger(CoolingService.class);
    private Connection connection;

    @Override
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    @SuppressWarnings("unused")
    private Connection useConnection() {
        if (connection == null) {
            throw new DataException("Connection not set");
        }
        return connection;
    }

    @Override
    public void transferSample(Integer sampleId, Integer diameterInCM) {

        if (!isSampleIdExisting(sampleId)) {
            throw new CoolingSystemException();
        }

        LocalDate sampleExpirationDate =
                getSampleExpirationDate(sampleId);

        if (!existsTrayDiameter(diameterInCM)) {
            throw new CoolingSystemException();
        }

        Integer trayId =
                findSuitableTray(diameterInCM, sampleExpirationDate);
        if (trayId == null) {

            trayId = findEmptyTray(diameterInCM);

            if (trayId == null) {
                throw new CoolingSystemException();
            }

            updateTrayExpirationDate(
                    trayId,
                    sampleExpirationDate.plusDays(30));
        }
        Integer placeNo = findFirstFreePlace(trayId);

        insertPlace(trayId, placeNo, sampleId);
    }


    public boolean isSampleIdExisting(Integer sampleId) {

        PreparedStatement pStmt = null;
        ResultSet rs = null;
        String query = "Select count(sampleid) as ANZAHL from sample where sampleid = ? ";

        try {
            pStmt = useConnection().prepareStatement(query);
            pStmt.setInt(1, sampleId);
            rs = pStmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("ANZAHL") > 0;
            } else {
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private LocalDate getSampleExpirationDate(Integer sampleId) {
        String sql =
                "SELECT expirationdate FROM sample WHERE sampleid = ?";

        try (PreparedStatement p = useConnection().prepareStatement(sql)) {
            p.setInt(1, sampleId);

            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    return rs.getDate("expirationdate").toLocalDate();
                }
            }
        } catch (SQLException e) {
            throw new DataException(e);
        }

        throw new CoolingSystemException("Sample nicht gefunden");
    }

    private boolean existsTrayDiameter(Integer diameterInCM) {

        PreparedStatement pStmt = null;
        ResultSet rs = null;

        String sql =
                "SELECT COUNT(TRAYID) AS ANZAHL " +
                        "FROM TRAY " +
                        "WHERE DIAMETERINCM = ?";

        try {

            pStmt = useConnection().prepareStatement(sql);
            pStmt.setInt(1, diameterInCM);

            rs = pStmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("ANZAHL") > 0;
            }

            return false;

        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    private Integer findSuitableTray(Integer diameterInCM,
                                     LocalDate sampleExpirationDate) {

        PreparedStatement pStmt = null;
        ResultSet rs = null;

        String sql =
                "SELECT trayid " +
                        "FROM tray " +
                        "WHERE diameterincm = ? " +
                        "AND expirationdate > ? " +
                        "ORDER BY expirationdate";

        try {

            pStmt = useConnection().prepareStatement(sql);

            pStmt.setInt(1, diameterInCM);
            pStmt.setDate(2, java.sql.Date.valueOf(sampleExpirationDate));

            rs = pStmt.executeQuery();

            while (rs.next()) {

                Integer trayId = rs.getInt("trayid");

                if (hasFreeCapacity(trayId)) {
                    return trayId;
                }
            }

            return null;

        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    private boolean hasFreeCapacity(Integer trayId) {

        PreparedStatement pStmt = null;
        ResultSet rs = null;

        String sql =
                "SELECT t.capacity, COUNT(p.sampleid) AS occupied " +
                        "FROM tray t " +
                        "LEFT JOIN place p ON t.trayid = p.trayid " +
                        "WHERE t.trayid = ? " +
                        "GROUP BY t.capacity";

        try {

            pStmt = useConnection().prepareStatement(sql);
            pStmt.setInt(1, trayId);

            rs = pStmt.executeQuery();

            if (rs.next()) {

                int capacity = rs.getInt("capacity");
                int occupied = rs.getInt("occupied");

                return occupied < capacity;
            }

            return false;

        } catch (SQLException e) {
            throw new DataException(e);
        }
    }
    private Integer findEmptyTray(Integer diameterInCM) {

        PreparedStatement pStmt = null;
        ResultSet rs = null;

        String sql =
                "SELECT t.trayid " +
                        "FROM tray t " +
                        "LEFT JOIN place p ON t.trayid = p.trayid " +
                        "WHERE t.diameterincm = ? " +
                        "GROUP BY t.trayid " +
                        "HAVING COUNT(p.sampleid) = 0 " +
                        "ORDER BY t.trayid";

        try {

            pStmt = useConnection().prepareStatement(sql);
            pStmt.setInt(1, diameterInCM);

            rs = pStmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("trayid");
            }

            return null;

        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    private void updateTrayExpirationDate(Integer trayId,
                                          LocalDate expirationDate) {

        PreparedStatement pStmt = null;

        String sql =
                "UPDATE tray " +
                        "SET expirationdate = ? " +
                        "WHERE trayid = ?";

        try {

            pStmt = useConnection().prepareStatement(sql);

            pStmt.setDate(
                    1,
                    java.sql.Date.valueOf(expirationDate));

            pStmt.setInt(2, trayId);

            pStmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    private Integer findFirstFreePlace(Integer trayId) {

        PreparedStatement pStmt = null;
        ResultSet rs = null;

        String sql =
                "SELECT placeno " +
                        "FROM place " +
                        "WHERE trayid = ? " +
                        "ORDER BY placeno";

        try {

            pStmt = useConnection().prepareStatement(sql);
            pStmt.setInt(1, trayId);

            rs = pStmt.executeQuery();

            int expectedPlaceNo = 1;

            while (rs.next()) {

                int currentPlaceNo = rs.getInt("placeno");

                if (currentPlaceNo != expectedPlaceNo) {
                    return expectedPlaceNo;
                }

                expectedPlaceNo++;
            }

            return expectedPlaceNo;

        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    private void insertPlace(Integer trayId,
                             Integer placeNo,
                             Integer sampleId) {

        PreparedStatement pStmt = null;

        String sql =
                "INSERT INTO place (trayid, placeno, sampleid) " +
                        "VALUES (?, ?, ?)";

        try {

            pStmt = useConnection().prepareStatement(sql);

            pStmt.setInt(1, trayId);
            pStmt.setInt(2, placeNo);
            pStmt.setInt(3, sampleId);

            pStmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

}
