package com.example.qrhunter.data.repository;

import com.example.qrhunter.data.model.QRCode;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QRCodeRepository extends DataRepository {
    /**
     * Create a QR Code document in Firestore
     */
    public void addQRCode(QRCode qrCode, String playerId) {
        PlayerRepository playerRepository = new PlayerRepository();

        // Check whether qr code exists.
        getQRCode(qrCode, documentSnapshot -> {
            if (documentSnapshot == null) {
                // If qr code doesn't exist, add a new document to Firestore
                Map<String, Object> qrCodeHashMap = new HashMap<>();
                qrCodeHashMap.put("hash", qrCode.getHash());
                qrCodeHashMap.put("name", qrCode.getName());
                qrCodeHashMap.put("visualRepresentation", qrCode.getVisualRepresentation());
                qrCodeHashMap.put("score", qrCode.getScore());
                qrCodeHashMap.put("latitude", qrCode.getLocation().latitude);
                qrCodeHashMap.put("longitude", qrCode.getLocation().longitude);
                // TODO: Upload location photo to firebase storage
                qrCodeHashMap.put("comments", new ArrayList<String>());

                db.collection("qrCodes").add(qrCodeHashMap).addOnCompleteListener(task -> {
                    playerRepository.addQRCodeToPlayer(playerId, task.getResult().getId());
                });
            } else {
                playerRepository.addQRCodeToPlayer(playerId, documentSnapshot.getId());
            }
        });
    }

    /**
     * Get a qr code document in Firestore.
     * The criteria for equivalence is based on https://eclass.srv.ualberta.ca/mod/forum/discuss.php?d=2203362#p5702650
     * Where a QRCode with location will be treated as a different QRCode with location even if their hash is the same.
     *
     * @param qrCode The qr code to be checked against
     * @return A QRCode document reference in the callback if qr code exist
     */
    public void getQRCode(QRCode qrCode, RepositoryCallback<DocumentSnapshot> repositoryCallback) {
        Query qrCodesQuery;

        if (qrCode.getLocation().longitude == 0 && qrCode.getLocation().latitude == 0) {
            // If qr code does not have geolocation
            qrCodesQuery = db.collection("qrCodes").whereEqualTo("latitude", 0)
                    .whereEqualTo("longitude", 0)
                    .whereEqualTo("hash", qrCode.getHash());
        } else {
            // If qr code have geolocation, doesn't matter where the location is
            qrCodesQuery = db.collection("qrCodes").whereNotEqualTo("latitude", 0)
                    .whereNotEqualTo("longitude", 0)
                    .whereEqualTo("hash", qrCode.getHash());
        }

        qrCodesQuery.get().addOnCompleteListener(task -> {
            //If qr code doesn't exist
            if (task.isSuccessful() && task.getResult().isEmpty()) {
                repositoryCallback.onSuccess(null);
            } else {
                repositoryCallback.onSuccess(task.getResult().getDocuments().get(0));
            }
        });
    }

}
