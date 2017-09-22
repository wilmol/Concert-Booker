package nz.ac.auckland.concert.service.services;

import nz.ac.auckland.concert.common.dto.ConcertDTO;
import nz.ac.auckland.concert.common.dto.PerformerDTO;
import nz.ac.auckland.concert.common.dto.UserDTO;
import nz.ac.auckland.concert.common.util.DataVerifier;
import nz.ac.auckland.concert.service.domain.Concert;
import nz.ac.auckland.concert.service.domain.Performer;
import nz.ac.auckland.concert.service.domain.User;
import nz.ac.auckland.concert.service.services.mappers.ConcertMapper;
import nz.ac.auckland.concert.service.services.mappers.PerformerMapper;
import nz.ac.auckland.concert.service.services.mappers.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static nz.ac.auckland.concert.common.config.CookieConfig.CLIENT_COOKIE;
import static nz.ac.auckland.concert.common.config.URIConfig.CONCERTS_URI;
import static nz.ac.auckland.concert.common.config.URIConfig.PERFORMERS_URI;
import static nz.ac.auckland.concert.common.config.URIConfig.USERS_URI;

/**
 * JAX-RS Resource class for the Concert Web service.
 * Defines a REST interface.
 * Supports the following HTTP messages:
 *
 * @author Will Molloy
 * @GET
 * @POST
 * @DELETE
 * @
 */
@Path("/concert")
@Produces({javax.ws.rs.core.MediaType.APPLICATION_XML})
@Consumes({javax.ws.rs.core.MediaType.APPLICATION_XML})
public class ConcertResource {

    private static Logger logger = LoggerFactory
            .getLogger(ConcertResource.class);

    private EntityManager entityManager = PersistenceManager.instance().createEntityManager();

    /**
     * Begins EntityManager transaction
     */
    private void beginTransaction() {
        entityManager.getTransaction().begin();
    }

    /**
     * Ends EntityManager transaction and close.
     * Call after beginTransaction().
     */
    private void commitTransaction() {
        entityManager.getTransaction().commit();
    }

    @Path(CONCERTS_URI)
    @GET
    public Response retrieveAllConcerts(@CookieParam(CLIENT_COOKIE) Cookie cookie) {
        logger.info("Retrieving all concerts.");

        beginTransaction();
        List<Concert> concerts = entityManager.createQuery("SELECT c FROM Concert c", Concert.class).getResultList();
        commitTransaction();

        Set<ConcertDTO> concertDTOS = new HashSet<>();
        concertDTOS.addAll(concerts.stream().map(ConcertMapper::toDto).collect(Collectors.toSet()));

        GenericEntity<Set<ConcertDTO>> entity = new GenericEntity<Set<ConcertDTO>>(concertDTOS) {
        };
        return Response.ok(entity)
                .cookie(makeCookie(cookie))
                .build();
    }

    @Path(PERFORMERS_URI)
    @GET
    public Response retrieveAllPerformers(@CookieParam(CLIENT_COOKIE) Cookie cookie) {
        logger.info("Retrieving all performers.");

        beginTransaction();
        List<Performer> performers = entityManager.createQuery("SELECT p FROM Performer p", Performer.class).getResultList();
        commitTransaction();

        Set<PerformerDTO> performerDTOS = new HashSet<>();
        performerDTOS.addAll(performers.stream().map(PerformerMapper::toDto).collect(Collectors.toSet()));

        GenericEntity<Set<PerformerDTO>> entity = new GenericEntity<Set<PerformerDTO>>(performerDTOS) {
        };
        return Response.ok(entity) // 200 OK status
                .cookie(makeCookie(cookie))
                .build();
    }

    @Path(USERS_URI)
    @POST
    public Response createUser(UserDTO userDTO, @CookieParam(CLIENT_COOKIE) Cookie cookie){
        logger.info("Attempting to persist new user: " + userDTO.getUsername());

        // Check all fields have been set
        if (!DataVerifier.allFieldsAreSet(userDTO)){
            throw new WebApplicationException(Response.Status.BAD_REQUEST); // 400 Bad Request status
        }

        // Check if username already persists
        beginTransaction();
        List<User> users = entityManager.createQuery("SELECT u FROM User u", User.class).getResultList();
        commitTransaction();
        if (users.stream().anyMatch(user -> user.getUsername().equals(userDTO.getUsername()))){
            throw new WebApplicationException(Response.Status.CONFLICT); // 409 Conflict status
        }

        // Persist the new user
        beginTransaction();
        entityManager.persist(UserMapper.toDomain(userDTO));
        commitTransaction();

        logger.info("Persisted new user: " + userDTO.getUsername());

        return Response.created(URI.create(USERS_URI + "/" + userDTO.getUsername())) // 201 Created status
                .cookie(makeCookie(cookie))
                .build();
    }

    /**
     * TODO Cookie goes into default service...???? Default service would have to pass it to here ????
     *
     * @param cookie
     * @return
     */
    private NewCookie makeCookie(Cookie cookie) {
        NewCookie newCookie;
        if (cookie == null) {
            newCookie = new NewCookie(CLIENT_COOKIE, UUID.randomUUID().toString());
            logger.info("Generated cookie: " + newCookie.getValue());
        } else {
            newCookie = new NewCookie(cookie);
        }
        return newCookie;
    }

}
