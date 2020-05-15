package de.probstl.ausgaben.data;

/**
 * Form for storing selection in Landing page
 */
public class HomeForm {

	/** The selected month to continue */
	private String m_SelectedMonth;

	/**
	 * @return the m_SelectedMonth
	 */
	public String getSelectedMonth() {
		return m_SelectedMonth;
	}

	/**
	 * @param m_SelectedMonth the m_SelectedMonth to set
	 */
	public void setSelectedMonth(String selectedMonth) {
		this.m_SelectedMonth = selectedMonth;
	}
}
