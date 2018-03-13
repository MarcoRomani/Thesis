package stCPP;
import java.util.ArrayList;
import java.util.List;

import general.*;


public interface DC_filler {

	public void populate(DataCenter dc,List<Customer> req, float tolerance) throws PopulateException;
	
}
