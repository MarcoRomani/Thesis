package ltCMP;

import java.util.List;

import general.CMPDataCenter;
import general.Customer;
import stCPP.PopulateException;

public interface CMPFiller {

	public void populate(CMPDataCenter dc, List<Customer> app, float tolerance) throws PopulateException;
}
