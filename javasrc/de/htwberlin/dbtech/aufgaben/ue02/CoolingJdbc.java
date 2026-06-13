package de.htwberlin.dbtech.aufgaben.ue02;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

import de.htwberlin.dbtech.exceptions.CoolingSystemException;
import de.htwberlin.dbtech.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.htwberlin.dbtech.exceptions.DataException;

public class CoolingJdbc implements ICoolingJdbc {

    private static final Logger L = LoggerFactory.getLogger(CoolingJdbc.class);
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
    public List<String> getSampleKinds() {
        L.info("getSampleKinds: start");
        List<String> sampleKind = null;
        String sql= "SELECT Text From samplekind order by Text asc";

        PreparedStatement p = null;
        ResultSet rs = null;

        try {
            sampleKind = new LinkedList<String>();
            p = useConnection().prepareStatement(sql);
            rs  = p.executeQuery();

            while(rs.next()) {
                sampleKind.add(rs.getString("text"));
            }

        }catch (SQLException e) {
            throw new DataException(e);
        }

        return sampleKind;
    }

    @Override
    public Sample findSampleById(Integer sampleId) {
        L.info("findSampleById: sampleId: " + sampleId);
        String sql = "SELECT * FROM sample WHERE sampleid = ?";

        PreparedStatement p = null;
        ResultSet rs = null;
        Sample sample = null;

        try {
            p = useConnection().prepareStatement(sql);
            p.setInt(1, sampleId);
            rs = p.executeQuery();

            if (rs.next()) {
                sample = new Sample();
                sample.setSampleId(rs.getInt("sampleid"));
                sample.setExpirationDate(DateUtils.sqlDate2LocalDate(rs.getDate("expirationdate")));
            } else {
                throw new CoolingSystemException("Sample nicht gefunden: " + sampleId);
            }

        } catch (SQLException e) {
            throw new DataException(e);
        }

        return sample;
    }

    @Override
    public void createSample(Integer sampleId, Integer sampleKindId) {
        L.info("createSample: sampleId: " + sampleId + ", sampleKindId: " + sampleKindId);
        String sqlKind = "SELECT validnoofdays FROM samplekind WHERE samplekindid = ?";
        String sqlCreate = "INSERT INTO sample (sampleid, samplekindid, expirationdate)" +
                            "VALUES (?,?,?)";

        PreparedStatement p = null;
        ResultSet rs = null;

        try {
            p = useConnection().prepareStatement(sqlKind);
            p.setInt(1,sampleKindId);
            rs = p.executeQuery();

            if (!rs.next()) {
                throw new CoolingSystemException("SampleKind nicht gefunden: " + sampleKindId);
            }

            int validNoOfDays = rs.getInt("validnoofdays");
            LocalDate expirationDate = LocalDate.now().plusDays(validNoOfDays);

            p = useConnection().prepareStatement(sqlCreate);
            p.setInt(1,sampleId);
            p.setInt(2,sampleKindId);
            p.setDate(3, DateUtils.localDate2SqlDate(expirationDate));
            p.executeUpdate();


        } catch (SQLException e) {

            if (e.getErrorCode() == 1) {
                throw new CoolingSystemException("Es konnte kein Sample mit Sampleid = " + sampleId + " erstellt werden");
            }
            throw new DataException(e);
        }
    }

    @Override
    public void clearTray(Integer trayId) {
        L.info("clearTray: trayId: " + trayId);
        // TODO Auto-generated method stub

        String sqlCheckTray = "SELECT trayid FROM tray WHERE trayid = ?";


        String sqlGetSamples = "SELECT sampleid FROM place WHERE trayid = ?";


        String sqlDeletePlaces = "DELETE FROM place WHERE trayid = ?";


        String sqlDeleteSample = "DELETE FROM sample WHERE sampleid = ?";

        PreparedStatement p = null;
        ResultSet rs = null;
        List<Integer> sampleIdsToDelete = new LinkedList<>();

        try {

            p = useConnection().prepareStatement(sqlCheckTray);
            p.setInt(1, trayId);
            rs = p.executeQuery();

            if (!rs.next()) {
                throw new CoolingSystemException("Tablett mit ID " + trayId + " existiert nicht.");
            }
            rs.close();
            p.close();


            p = useConnection().prepareStatement(sqlGetSamples);
            p.setInt(1, trayId);
            rs = p.executeQuery();
            while (rs.next()) {
                sampleIdsToDelete.add(rs.getInt("sampleid"));
            }
            rs.close();
            p.close();


            p = useConnection().prepareStatement(sqlDeletePlaces);
            p.setInt(1, trayId);
            p.executeUpdate();
            p.close();

            p = useConnection().prepareStatement(sqlDeleteSample);
            for (Integer sampleId : sampleIdsToDelete) {
                p.setInt(1, sampleId);
                p.executeUpdate();
            }

        } catch (SQLException e) {
            throw new DataException(e);
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { L.error("Fehler ResultSet", e); }
            try { if (p != null) p.close(); } catch (SQLException e) { L.error("Fehler PreparedStatement", e); }
        }
    }

}
