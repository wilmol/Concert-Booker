package nz.ac.auckland.concert.client.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import nz.ac.auckland.concert.common.dto.*;
import nz.ac.auckland.concert.common.message.Messages;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.io.File;
import java.util.Set;

import static nz.ac.auckland.concert.common.config.URIConfig.*;

public class DefaultService implements ConcertService {

    // AWS S3 access credentials for concert images.
    private static final String AWS_ACCESS_KEY_ID = "AKIAIDYKYWWUZ65WGNJA";
    private static final String AWS_SECRET_ACCESS_KEY = "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";
    // Name of the S3 bucket that stores images.
    private static final String AWS_BUCKET = "concert.aucklanduni.ac.nz";

    private Client client;
    private Response response;

    /**
     * Creates a new client connection
     */
    private void createNewClientConnection() {
        client = ClientBuilder.newClient();
    }

    /**
     * Closes the response and client connections
     */
    private void closeResponseAndClient() {
        response.close();
        client.close();
    }

    /**
     * @see ConcertService#getConcerts()
     */
    @Override
    public Set<ConcertDTO> getConcerts() throws ServiceException {
        Set<ConcertDTO> concerts;
        try {
            createNewClientConnection();

            Builder builder = client.target(WEB_SERVICE_URI + CONCERTS_URI).request().accept(MediaType.APPLICATION_XML);
            response = builder.get();

            concerts = response
                    .readEntity(new GenericType<Set<ConcertDTO>>() {
                    });
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
        } finally {
            closeResponseAndClient();
        }
        return concerts;
    }

    /**
     * @see ConcertService#getPerformers()
     */
    @Override
    public Set<PerformerDTO> getPerformers() throws ServiceException {
        Set<PerformerDTO> performers;
        try {
            createNewClientConnection();

            Builder builder = client.target(WEB_SERVICE_URI + PERFORMERS_URI).request().accept(MediaType.APPLICATION_XML);
            response = builder.get();

            performers = response
                    .readEntity(new GenericType<Set<PerformerDTO>>() {
                    });
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
        } finally {
            closeResponseAndClient();
        }
        return performers;
    }

    /**
     * @see ConcertService#createUser(UserDTO)
     */
    @Override
    public UserDTO createUser(UserDTO newUser) throws ServiceException {
        int status;
        try {
            createNewClientConnection();

            Builder builder = client.target(WEB_SERVICE_URI + USERS_URI).request();

            response = builder
                    .post(Entity.entity(newUser, MediaType.APPLICATION_XML));

            status = response.getStatus();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
        } finally {
            closeResponseAndClient();
        }

        if (status == Response.Status.CONFLICT.getStatusCode()) {
            throw new ServiceException(Messages.CREATE_USER_WITH_NON_UNIQUE_NAME);

        } else if (status == Response.Status.BAD_REQUEST.getStatusCode()) {
            throw new ServiceException(Messages.CREATE_USER_WITH_MISSING_FIELDS);
        }

        return newUser; // "identity property is also set", username is that property...? TODO I guess that means the cookie / Authentication token!!
    }

    /**
     * @see ConcertService#authenticateUser(UserDTO)
     */
    @Override
    public UserDTO authenticateUser(UserDTO user) throws ServiceException {
        UserDTO authenticatedUser;
        int status;
        try {
            createNewClientConnection();

            Builder builder = client.target(WEB_SERVICE_URI + USERS_URI + LOGIN_URI).request().accept(MediaType.APPLICATION_XML);

            response = builder
                    .post(Entity.entity(user, MediaType.APPLICATION_XML));

            status = response.getStatus();

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
        } finally {
            closeResponseAndClient();
        }
        if (status == Response.Status.BAD_REQUEST.getStatusCode()) {
            throw new ServiceException(Messages.AUTHENTICATE_USER_WITH_MISSING_FIELDS);

        } else if (status == Response.Status.NOT_FOUND.getStatusCode()) {
            throw new ServiceException(Messages.AUTHENTICATE_NON_EXISTENT_USER);

        } else if (status == Response.Status.UNAUTHORIZED.getStatusCode()) {
            throw new ServiceException(Messages.AUTHENTICATE_USER_WITH_ILLEGAL_PASSWORD);
        }

        authenticatedUser = response.readEntity(UserDTO.class);
        return authenticatedUser;
    }

    @Override
    public Image getImageForPerformer(PerformerDTO performer) throws ServiceException {
        String imageName = performer.getImageName();
        File imageFile = new File(imageName);

        TransferManager mgr = null;
        try {
            // Create an AmazonS3 object that represents a connection with the
            // remote S3 service.
            BasicAWSCredentials awsCredentials = new BasicAWSCredentials(
                    AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY);

            // Setup the client
            AmazonS3 s3 = AmazonS3ClientBuilder
                    .standard()
                    .withRegion(Regions.AP_SOUTHEAST_2)
                    .withCredentials(
                            new AWSStaticCredentialsProvider(awsCredentials))
                    .build();

            // Setup the transfer
            mgr = TransferManagerBuilder
                    .standard()
                    .withS3Client(s3)
                    .build();

            // Download the image
            Download download = mgr.download(AWS_BUCKET, imageName, imageFile);
            download.waitForCompletion();

        } catch (Exception e) {
            throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
        } finally {
            mgr.shutdownNow();
        }

        return Toolkit.getDefaultToolkit().getImage(imageName);
    }

    @Override
    public ReservationDTO reserveSeats(ReservationRequestDTO reservationRequest) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void confirmReservation(ReservationDTO reservation) throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void registerCreditCard(CreditCardDTO creditCard) throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<BookingDTO> getBookings() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void subscribeForNewsItems(NewsItemListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelSubscription() {
        throw new UnsupportedOperationException();
    }

}
