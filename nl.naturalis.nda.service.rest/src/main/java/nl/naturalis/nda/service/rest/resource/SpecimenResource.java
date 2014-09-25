package nl.naturalis.nda.service.rest.resource;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import nl.naturalis.nda.domain.Specimen;
import nl.naturalis.nda.ejb.service.SpecimenService;
import nl.naturalis.nda.elasticsearch.dao.dao.SpecimenDao;
import nl.naturalis.nda.elasticsearch.dao.util.QueryParams;
import nl.naturalis.nda.search.SearchResultSet;

@Path("/specimen")
@Stateless
@LocalBean
/* only here so @EJB injection works in JBoss AS; remove when possible */
public class SpecimenResource {

	@EJB
	SpecimenService service;

	@EJB
	Registry registry;
	
	


	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResultSet search(@Context UriInfo request)
	{
		SpecimenDao dao = new SpecimenDao(registry.getESClient(), "nda");
		QueryParams params = new QueryParams(request.getQueryParameters());
		SearchResultSet rs = new SearchResultSet<Specimen>();
		return rs;
	}
	

}
