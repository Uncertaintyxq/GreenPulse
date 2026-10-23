package com.greenpulse.service;

import com.greenpulse.dao.CollectionDAO;
import com.greenpulse.dao.SmartBinDAO;
import com.greenpulse.event.BinEventBus;
import com.greenpulse.exception.InvalidFillLevelException;
import com.greenpulse.model.*;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Business rules for changing bins. Every method checks the signed-in user's {@link Role} first, so permissions
 * are enforced here and not only by hiding buttons in the interface.
 */
public class BinService {
    private final User user;
    private final SmartBinDAO binDAO;
    private final CollectionDAO collectionDAO;
    private final BinEventBus eventBus = BinEventBus.getInstance();

    public BinService(User user) {
        this(user, new SmartBinDAO(), new CollectionDAO());
    }

    public BinService(User user, SmartBinDAO binDAO, CollectionDAO collectionDAO) {
        if (user == null) throw new IllegalArgumentException("A signed-in user is required.");
        this.user = user;
        this.binDAO = binDAO;
        this.collectionDAO = collectionDAO;
    }

    public void registerBin(SmartBin bin) throws SQLException {
        require(user.getRoleType().canRegisterBins(), "register bins");
        binDAO.register(bin, user.getUserId());
    }

    /** Marks a bin as emptied. Returns the fill level it had before, or empty if the bin no longer exists. */
    public Optional<Double> markCollected(String binId, String routeId) throws SQLException, InvalidFillLevelException {
        require(user.getRoleType().canRecordCollections(), "record collections");
        Optional<Double> before = collectionDAO.recordCollection(binId, user.getUserId(), routeId);
        if (before.isPresent()) {
            binDAO.findById(binId).ifPresent(bin -> eventBus.publish(bin, ReadingSource.COLLECTION));
        }
        return before;
    }

    public boolean archiveBin(String binId) throws SQLException {
        require(user.getRoleType().canArchiveBins(), "remove bins");
        return binDAO.archive(binId, user.getUserId());
    }

    /** Saves a manually entered reading (used by the Sensor Simulation screen) and notifies listeners. */
    public boolean recordManualReading(SmartBin bin) throws SQLException {
        require(user.getRoleType().canRegisterBins(), "enter sensor readings");
        boolean saved = binDAO.recordReading(bin, ReadingSource.MANUAL);
        if (saved) eventBus.publish(bin, ReadingSource.MANUAL);
        return saved;
    }

    private void require(boolean allowed, String action) {
        if (!allowed) {
            throw new SecurityException("Your role (" + user.getRole() + ") is not allowed to " + action + ".");
        }
    }
}
