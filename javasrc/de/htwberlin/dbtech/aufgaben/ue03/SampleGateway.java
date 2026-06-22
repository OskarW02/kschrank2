package de.htwberlin.dbtech.aufgaben.ue03;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import de.htwberlin.dbtech.exceptions.CoolingSystemException;
import de.htwberlin.dbtech.exceptions.DataException;

public class SampleGateway {

    private final Connection connection;

    public SampleGateway(Connection connection) {
        this.connection = connection;
    }

    public boolean exists(Integer sampleId) {

        String sql = "SELECT COUNT(sampleid) AS ANZAHL FROM sample WHERE sampleid = ?";

        try (PreparedStatement pStmt = connection.prepareStatement(sql)) {

            pStmt.setInt(1, sampleId);

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

    public LocalDate getExpirationDate(Integer sampleId) {

        String sql = "SELECT expirationdate FROM sample WHERE sampleid = ?";

        try (PreparedStatement pStmt = connection.prepareStatement(sql)) {

            pStmt.setInt(1, sampleId);

            try (ResultSet rs = pStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDate("expirationdate").toLocalDate();
                }
            }

        } catch (SQLException e) {
            throw new DataException(e);
        }

        throw new CoolingSystemException("Sample nicht gefunden");
    }
}