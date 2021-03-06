/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.util.List;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.entity.Holder;
import org.greenpole.entity.model.holder.PowerOfAttorney;
import org.greenpole.hibernate.entity.AccountConsolidation;
import org.greenpole.hibernate.entity.Administrator;
import org.greenpole.hibernate.entity.AdministratorEmailAddress;
import org.greenpole.hibernate.entity.AdministratorPhoneNumber;
import org.greenpole.hibernate.entity.AdministratorResidentialAddress;
import org.greenpole.hibernate.entity.CompanyAccountConsolidation;
import org.greenpole.hibernate.entity.HolderBondAccount;
import org.greenpole.hibernate.entity.HolderCompanyAccount;

/**
 *
 * @author user
 */
public interface HibernatDummyQuerInterface {

    /**
     *
     * @param admin
     * @param emailAddress
     * @param phoneNumber
     * @param residentialAddress
     * @param holder
     */
    public void createAdministratorForShareHolderAndBondHolder(Administrator admin, List<AdministratorEmailAddress> emailAddress, List<AdministratorPhoneNumber> phoneNumber, List<AdministratorResidentialAddress> residentialAddress, List<Holder> holder);

    public void createEmail(AdministratorEmailAddress emailAddress);

    public void createPhoneNumber(AdministratorPhoneNumber phoneNumber);

    public void createAdministratorResidentialAddress(AdministratorResidentialAddress adminResidentialAddress);

    //public boolean checkHolder(Holder holder);
    /**
     *
     * @param holder
     */
    public void updateAdministrationHolderCompanyAccount(org.greenpole.hibernate.entity.Holder holder);

    public Holder retrieveHolderObject(int holderId);

    /**
     *
     * @param power
     */
    public void uploadPowerOfAttorney(PowerOfAttorney power);

    public boolean checkHolderNubanNumber(String nubanAccount);

    public List getAllShareholderNubanAccounts();

    public List getAllBondholderNubanAccounts();

    public void addShareholderNubanAccount();

    public void createNubanAccount(HolderCompanyAccount holderAccount);

    public void createBondNubanAccount(HolderBondAccount holderAccount);

    public void changeShareholderNubanAccount(HolderCompanyAccount holderAccount);

    public void changeBondholderNubanAccount(HolderBondAccount bondholderAccount);

    //public org.greenpole.hibernate.entity.HolderChanges getHolderEditedDetails(int holderId);
    //public org.greenpole.hibernate.entity.HolderChanges retrieveHolderChangesQueryOne(String changeType, String changeDate, int holderId);
   // public org.greenpole.hibernate.entity.HolderChanges retrieveHolderChangesQueryTwo(String changeType, String changeDate1, String changeDate2, int holderId);
    public org.greenpole.hibernate.entity.HolderCompanyAccount retrieveHolderCompanyAccount(int holderId, int clientCompanyId);

    public org.greenpole.hibernate.entity.HolderBondAccount retrieveHolderBondCompAccount(int holderId, int bondId);

    public List<CompanyAccountConsolidation> queryAccountConsolidation(String descriptor, CompanyAccountConsolidation compAccCon, String start_date, String end_date);

    public List<Login> getUserList(List<Login> login);

    public List<AccountConsolidation> queryAccCon(int holderId);

    public boolean updatePowerOfAttorneyStatus(int holderId);

    public org.greenpole.hibernate.entity.PowerOfAttorney retrieveCurrentPowerOfAttorney(int holderId);
}
