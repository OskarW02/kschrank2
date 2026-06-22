package de.htwberlin.dbtech.aufgaben.ue03;

import java.sql.Connection;
import java.time.LocalDate;

import de.htwberlin.dbtech.exceptions.CoolingSystemException;
import de.htwberlin.dbtech.exceptions.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoolingServiceDao implements ICoolingService {

    private static final Logger L = LoggerFactory.getLogger(CoolingServiceDao.class);

    private Connection connection;

    private SampleGateway sampleGateway;
    private TrayGateway trayGateway;
    private PlaceGateway placeGateway;

    @Override
    public void setConnection(Connection connection) {
        this.connection = connection;
        this.sampleGateway = new SampleGateway(connection);
        this.trayGateway = new TrayGateway(connection);
        this.placeGateway = new PlaceGateway(connection);
    }

    @Override
    public void transferSample(Integer sampleId, Integer diameterInCM) {

        if (!sampleGateway.exists(sampleId)) {
            throw new CoolingSystemException();
        }

        LocalDate sampleExpirationDate = sampleGateway.getExpirationDate(sampleId);

        if (!trayGateway.existsDiameter(diameterInCM)) {
            throw new CoolingSystemException();
        }

        Integer trayId = findSuitableTray(diameterInCM, sampleExpirationDate);

        if (trayId == null) {

            trayId = trayGateway.findEmptyTray(diameterInCM);

            if (trayId == null) {
                throw new CoolingSystemException();
            }

            trayGateway.updateExpirationDate(trayId, sampleExpirationDate.plusDays(30));
        }

        Integer placeNo = placeGateway.findFirstFreePlace(trayId);

        placeGateway.insertPlace(trayId, placeNo, sampleId);
    }

    private Integer findSuitableTray(Integer diameterInCM, LocalDate sampleExpirationDate) {

        for (Integer trayId : trayGateway.findCandidateTrays(diameterInCM, sampleExpirationDate)) {
            if (placeGateway.hasFreeCapacity(trayId)) {
                return trayId;
            }
        }
        return null;
    }
}