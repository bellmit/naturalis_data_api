package nl.naturalis.nba.rest.resource;

import static nl.naturalis.nba.rest.util.ResourceUtil.JSON_CONTENT_TYPE;
import static nl.naturalis.nba.rest.util.ResourceUtil.handleError;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.naturalis.nba.api.model.ObjectType;
import nl.naturalis.nba.api.model.Specimen;
import nl.naturalis.nba.api.query.QuerySpec;
import nl.naturalis.nba.common.json.JsonUtil;
import nl.naturalis.nba.dao.es.SpecimenDAO;
import nl.naturalis.nba.rest.exception.HTTP404Exception;
import nl.naturalis.nda.ejb.service.SpecimenService;

@Path("/specimen")
@Stateless
@LocalBean
/* only here so @EJB injection works in JBoss AS; remove when possible */
public class SpecimenResource {

	@SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger(SpecimenResource.class);

	@EJB
	SpecimenService service;

	@EJB
	Registry registry;

	@GET
	@Path("/find/{id}")
	@Produces(JSON_CONTENT_TYPE)
	public Specimen find(@PathParam("id") String id, @Context UriInfo uriInfo)
	{
		try {
			SpecimenDAO dao = new SpecimenDAO();
			Specimen result = dao.find(id);
			if (result == null) {
				throw new HTTP404Exception(uriInfo, ObjectType.SPECIMEN, id);
			}
			return result;
		}
		catch (Throwable t) {
			throw handleError(uriInfo, t);
		}
	}

	@GET
	@Path("/findByUnitID/{unitID}")
	@Produces(JSON_CONTENT_TYPE)
	public Specimen[] findByUnitID(@PathParam("unitID") String unitID, @Context UriInfo uriInfo)
	{
		try {
			SpecimenDAO dao = new SpecimenDAO();
			return dao.findByUnitID(unitID);
		}
		catch (Throwable t) {
			throw handleError(uriInfo, t);
		}
	}

	@GET
	@Path("/exists/{unitID}")
	@Produces(JSON_CONTENT_TYPE)
	public boolean exists(@PathParam("unitID") String unitID, @Context UriInfo uriInfo)
	{
		try {
			SpecimenDAO dao = new SpecimenDAO();
			return dao.exists(unitID);
		}
		catch (Throwable t) {
			throw handleError(uriInfo, t);
		}
	}

	@GET
	@Path("/query/{querySpec}")
	@Produces(JSON_CONTENT_TYPE)
	public Specimen[] query(@PathParam("querySpec") String json, @Context UriInfo uriInfo)
	{
		try {
			QuerySpec qs = JsonUtil.fromJson(json, QuerySpec.class);
			SpecimenDAO dao = new SpecimenDAO();
			return dao.query(qs);
		}
		catch (Throwable t) {
			throw handleError(uriInfo, t);
		}
	}

}