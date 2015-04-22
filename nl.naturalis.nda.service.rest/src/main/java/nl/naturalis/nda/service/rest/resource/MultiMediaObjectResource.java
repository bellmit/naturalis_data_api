package nl.naturalis.nda.service.rest.resource;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import nl.naturalis.nda.domain.MultiMediaObject;
import nl.naturalis.nda.elasticsearch.dao.dao.BioportalMultiMediaObjectDao;
import nl.naturalis.nda.search.QueryParams;
import nl.naturalis.nda.search.SearchResultSet;
import nl.naturalis.nda.service.rest.util.NDA;
import nl.naturalis.nda.service.rest.util.ResourceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/multimedia")
@Stateless
@LocalBean
/* only here so @EJB injection works in JBoss AS; remove when possible */
public class MultiMediaObjectResource {

	private static final Logger logger = LoggerFactory.getLogger(MultiMediaObjectResource.class);

	@EJB
	Registry registry;


	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResultSet<MultiMediaObject> searchGET(@Context UriInfo uriInfo, @Context HttpServletRequest request)
	{
		try {
			logger.debug("searchGET");
			QueryParams params = new QueryParams(uriInfo.getQueryParameters());
			params.putSingle(NDA.SESSION_ID_PARAM, request.getSession().getId());
			String baseUrl = uriInfo.getBaseUri().toString();
			BioportalMultiMediaObjectDao dao = registry.getBioportalMultiMediaObjectDao(baseUrl);
			SearchResultSet<MultiMediaObject> result = dao.multiMediaObjectSearch(params);
			ResourceUtil.doAfterDao(result, uriInfo, true);
			return result;
		}
		catch (Throwable t) {
			throw ResourceUtil.handleError(uriInfo, t);
		}
	}


	@POST
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public SearchResultSet<MultiMediaObject> searchPOST(@Context UriInfo uriInfo, MultivaluedMap<String, String> form, @Context HttpServletRequest request)
	{
		try {
			logger.debug("searchPOST");
			QueryParams params = new QueryParams(form);
			params.addParams(uriInfo.getQueryParameters());
			params.putSingle(NDA.SESSION_ID_PARAM, request.getSession().getId());
			String baseUrl = uriInfo.getBaseUri().toString();
			BioportalMultiMediaObjectDao dao = registry.getBioportalMultiMediaObjectDao(baseUrl);
			SearchResultSet<MultiMediaObject> result = dao.multiMediaObjectSearch(params);
			ResourceUtil.doAfterDao(result, uriInfo, form, true);
			return result;
		}
		catch (Throwable t) {
			throw ResourceUtil.handleError(uriInfo, form, t);
		}
	}


	@GET
	@Path("/get-multimedia-object-for-taxon-within-result-set")
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResultSet<MultiMediaObject> getTaxonMultiMediaObjectDetailWithinResultSet(@Context UriInfo uriInfo, @Context HttpServletRequest request)
	{
		try {
			logger.debug("getTaxonMultiMediaObjectDetailWithinResultSet");
			QueryParams params = new QueryParams(uriInfo.getQueryParameters());
			params.putSingle(NDA.SESSION_ID_PARAM, request.getSession().getId());
			String baseUrl = uriInfo.getBaseUri().toString();
			BioportalMultiMediaObjectDao dao = registry.getBioportalMultiMediaObjectDao(baseUrl);
			SearchResultSet<MultiMediaObject> result = dao.getTaxonMultiMediaObjectDetailWithinResultSet(params);
			ResourceUtil.doAfterDao(result, uriInfo, false);
			return result;
		}
		catch (Throwable t) {
			throw ResourceUtil.handleError(uriInfo, t);
		}
	}


	@GET
	@Path("/get-multimedia-object-for-specimen-within-result-set")
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResultSet<MultiMediaObject> getSpecimenMultiMediaObjectDetailWithinResultSet(@Context UriInfo uriInfo, @Context HttpServletRequest request)
	{
		try {
			logger.debug("getSpecimenMultiMediaObjectDetailWithinResultSet");
			QueryParams params = new QueryParams(uriInfo.getQueryParameters());
			params.putSingle(NDA.SESSION_ID_PARAM, request.getSession().getId());
			String baseUrl = uriInfo.getBaseUri().toString();
			BioportalMultiMediaObjectDao dao = registry.getBioportalMultiMediaObjectDao(baseUrl);
			SearchResultSet<MultiMediaObject> result = dao.getSpecimenMultiMediaObjectDetailWithinResultSet(params);
			ResourceUtil.doAfterDao(result, uriInfo, false);
			return result;
		}
		catch (Throwable t) {
			throw ResourceUtil.handleError(uriInfo, t);
		}
	}

}
