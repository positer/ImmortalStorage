package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.resource.list.MutableResourceListImpl;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorageImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RsNetworkDeduplicatorTest {
    @Test
    void sameOwnerHasOnePrimaryPerNetworkWhileIndependentNetworksStillWork() {
        UUID owner = UUID.fromString("20000000-0000-0000-0000-000000000001");

        XianqiaoRsStorage a1 = storage(owner, "00000000-0000-0000-0000-000000000002");
        XianqiaoRsStorage a2 = storage(owner, "00000000-0000-0000-0000-000000000001");
        CompositeStorageImpl rootA = networkWithSeparateDrives(a1, a2);
        RsNetworkDeduplicator.rebalanceFrom(rootA);

        assertEquals(1L, List.of(a1, a2).stream()
                .filter(XianqiaoRsStorage::isNetworkPrimary).count());
        assertFalse(a1.isNetworkPrimary());
        assertTrue(a2.isNetworkPrimary());

        XianqiaoRsStorage b1 = storage(owner, "00000000-0000-0000-0000-000000000004");
        XianqiaoRsStorage b2 = storage(owner, "00000000-0000-0000-0000-000000000003");
        CompositeStorageImpl rootB = networkWithSeparateDrives(b1, b2);
        RsNetworkDeduplicator.rebalanceFrom(rootB);

        assertEquals(1L, List.of(b1, b2).stream()
                .filter(XianqiaoRsStorage::isNetworkPrimary).count());
        assertTrue(a2.isNetworkPrimary(), "network B must not deactivate network A");
        assertTrue(b2.isNetworkPrimary());
    }

    @Test
    void removingThePrimaryPromotesTheRemainingSameOwnerDisk() {
        UUID owner = UUID.fromString("30000000-0000-0000-0000-000000000001");
        XianqiaoRsStorage first = storage(owner, "00000000-0000-0000-0000-000000000001");
        XianqiaoRsStorage second = storage(owner, "00000000-0000-0000-0000-000000000002");
        CompositeStorageImpl firstDrive = drive(first);
        CompositeStorageImpl secondDrive = drive(second);
        CompositeStorageImpl root = composite();
        root.addSource(firstDrive);
        root.addSource(secondDrive);
        RsNetworkDeduplicator.rebalanceFrom(root);
        assertTrue(first.isNetworkPrimary());
        assertFalse(second.isNetworkPrimary());

        root.removeSource(firstDrive);
        RsNetworkDeduplicator.rebalanceFrom(root);
        assertTrue(second.isNetworkPrimary());
    }

    @Test
    void differentOwnerUuidsCoexistAndDisplayNamesCannotParticipateInElection() {
        UUID firstOwner = UUID.fromString("40000000-0000-0000-0000-000000000001");
        UUID secondOwner = UUID.fromString("40000000-0000-0000-0000-000000000002");
        XianqiaoRsStorage first = storage(
                firstOwner, "00000000-0000-0000-0000-000000000001");
        XianqiaoRsStorage second = storage(
                secondOwner, "00000000-0000-0000-0000-000000000002");
        CompositeStorageImpl root = networkWithSeparateDrives(first, second);

        RsNetworkDeduplicator.rebalanceFrom(root);

        assertTrue(first.isNetworkPrimary());
        assertTrue(second.isNetworkPrimary());
        assertTrue(RsNetworkDeduplicator.containsOwner(root, firstOwner));
        assertTrue(RsNetworkDeduplicator.containsOwner(root, secondOwner));
    }

    @Test
    void removingInactiveDuplicateLeavesPrimaryMountedAndMakesRemovedDiskStandalone() {
        UUID owner = UUID.fromString("50000000-0000-0000-0000-000000000001");
        XianqiaoRsStorage primary = storage(
                owner, "00000000-0000-0000-0000-000000000001");
        XianqiaoRsStorage duplicate = storage(
                owner, "00000000-0000-0000-0000-000000000002");
        CompositeStorageImpl primaryDrive = drive(primary);
        CompositeStorageImpl duplicateDrive = drive(duplicate);
        CompositeStorageImpl root = composite();
        root.addSource(primaryDrive);
        root.addSource(duplicateDrive);
        RsNetworkDeduplicator.rebalanceFrom(root);
        assertTrue(primary.isNetworkPrimary());
        assertFalse(duplicate.isNetworkPrimary());

        root.removeSource(duplicateDrive);
        RsNetworkDeduplicator.rebalanceFrom(root);
        RsNetworkDeduplicator.rebalanceFrom(duplicateDrive);

        assertTrue(primary.isNetworkPrimary());
        assertTrue(duplicate.isNetworkPrimary(), "a removed disk must remain usable elsewhere");
    }

    private static CompositeStorageImpl networkWithSeparateDrives(
            XianqiaoRsStorage first, XianqiaoRsStorage second) {
        CompositeStorageImpl root = composite();
        root.addSource(drive(first));
        root.addSource(drive(second));
        return root;
    }

    private static CompositeStorageImpl drive(XianqiaoRsStorage storage) {
        CompositeStorageImpl drive = composite();
        drive.addSource(storage);
        return drive;
    }

    private static CompositeStorageImpl composite() {
        return new CompositeStorageImpl(MutableResourceListImpl.create());
    }

    private static XianqiaoRsStorage storage(UUID owner, String diskId) {
        return new XianqiaoRsStorage(owner, UUID.fromString(diskId));
    }
}
