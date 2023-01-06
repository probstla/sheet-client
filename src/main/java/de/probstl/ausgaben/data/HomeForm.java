package de.probstl.ausgaben.data;

/**
 * Form for storing selection in Landing page
 */
public class HomeForm {

	/** The selected month to continue */
	private String m_SelectedMonth;

	/** The entered text for the search */
	private String m_SearchText;

	/**
	 * @return the selectedMonth
	 */
	public String getSelectedMonth() {
		return m_SelectedMonth;
	}

	/**
	 * @param selectedMonth the selectedMonth to set
	 */
	public void setSelectedMonth(String selectedMonth) {
		m_SelectedMonth = selectedMonth;
	}

	/**
	 * @return Returns the searched text
	 */
	public String getSearchText() {
		return m_SearchText;
	}

	/**
	 * @param searchText Set the text that is searched for
	 */
	public void setSearchText(String searchText) {
		m_SearchText = searchText;
	}
}
