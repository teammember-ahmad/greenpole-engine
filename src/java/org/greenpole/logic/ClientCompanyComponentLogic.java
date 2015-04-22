/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.model.clientcompany.ClientCompany;
import org.greenpole.entity.model.clientcompany.QueryClientCompany;
import org.greenpole.entity.model.clientcompany.ShareQuotation;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.response.Response;
import org.greenpole.hibernate.entity.ClientCompanyAddress;
import org.greenpole.hibernate.entity.ClientCompanyAddressId;
import org.greenpole.hibernate.entity.ClientCompanyEmailAddressId;
import org.greenpole.hibernate.entity.ClientCompanyPhoneNumberId;
import org.greenpole.hibernate.entity.Depository;
import org.greenpole.hibernate.entity.NseSector;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Descriptor;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Akinwale Agbaje
 * @version 1.0
 * Business requirement implementations to do with client companies
 */
public class ClientCompanyComponentLogic {
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private static final Logger logger = LoggerFactory.getLogger(ClientCompanyComponentLogic.class);
    
    /**
     * Processes request to create a new client company.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param cc the client company to be created
     * @return response to the client company creation request 
     */
    public Response createClientCompany_Request(Login login, String authenticator, ClientCompany cc) {
        logger.info("request to create the client company [{}] from [{}]", cc.getName(), login.getUserId());
        
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        
        //check if client company exists
        if (!cq.checkClientCompany(cc.getName())) {
            wrapper = new NotificationWrapper();
            prop = new NotifierProperties(ClientCompanyComponentLogic.class);
            qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), 
                    prop.getAuthoriserNotifierQueueName());
            
            logger.info("client company does not exist - [{}]", cc.getName());
            List<ClientCompany> cclist = new ArrayList<>();
            cclist.add(cc);
            //wrap client company object in notification object, along with other information
            wrapper.setCode(Notification.createCode(login));
            wrapper.setDescription("Authenticate creation of the client company, " + cc.getName());
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(cclist);
            
            resp = qSender.sendAuthorisationRequest(wrapper);
            logger.info("notification fowarded to queue - notification code: [{}]", wrapper.getCode());
            return resp;
        }
        resp.setRetn(200);
        resp.setDesc("Client company already exists and so cannot be created.");
        logger.info("client company exists so cannot be created - [{}]: [{}]", cc.getName(), resp.getRetn());
        return resp;
    }
    
    /**
     * Processes request to create a client company that has already been saved 
     * as a notification file, according to the specified notification code.
     * @param notificationCode the notification code
     * @return response to the client company creation request
     */
    public Response createClientCompany_Authorise(String notificationCode) {
        Response resp = new Response();
        boolean freshCreation = true;
        logger.info("client company creation authorised - [{}]", notificationCode);
        try {
            //get client company model from wrapper
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<ClientCompany> list = (List<ClientCompany>) wrapper.getModel();
            ClientCompany ccModel = list.get(0);
            
            boolean created = cq.createOrUpdateClientCompany(retrieveClientCompanyModel(ccModel, freshCreation), retrieveAddressModel(ccModel, freshCreation), 
                    retrieveEmailAddressModel(ccModel, freshCreation), retrievePhoneNumberModel(ccModel, freshCreation));
            
            if (created) {
                logger.info("client company created - [{}]", ccModel.getName());
                resp.setRetn(0);
                resp.setDesc("Successful");
                return resp;
            }
            
            resp.setRetn(201);
            resp.setDesc("Unable to create client company from authorisation. Contact System Administrator");
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
            
            resp.setRetn(201);
            resp.setDesc("Unable to create client company from authorisation. Contact System Administrator");
            
            return resp;
        }
    }
    
    /**
     * Processes request to edit an existing client company.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param cc the client company to be created
     * @return response to the client company creation request
     */
    public Response editClientCompany_Request(Login login, String authenticator, ClientCompany cc) {
        logger.info("request to edit the client company [{}] from [{}]", cc.getName(), login.getUserId());
        
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        
        //client company must exist to be edited
        if (cq.checkClientCompany(cc.getName())) {
            wrapper = new NotificationWrapper();
            prop = new NotifierProperties(ClientCompanyComponentLogic.class);
            qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), 
                    prop.getAuthoriserNotifierQueueName());
            
            logger.info("client company exists - [{}]", cc.getName());
            List<ClientCompany> cclist = new ArrayList<>();
            cclist.add(cc);
            //wrap client company object in notification object, along with other information
            wrapper.setCode(Notification.createCode(login));
            wrapper.setDescription("Authenticate change to client company, " + cc.getName());
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(cclist);
            
            resp = qSender.sendAuthorisationRequest(wrapper);
            logger.info("notification fowarded to queue - notification code: [{}]", wrapper.getCode());
            return resp;
        }
        resp.setRetn(202);
        resp.setDesc("Client company does not exist, so cannot be edited.");
        logger.info("client company does not exist so cannot be edited - [{}]: [{}]", cc.getName(), resp.getRetn());
        return resp;
    }
    
    /**
     * Processes request to change a client company that has already been saved 
     * as a notification file, according to the specified notification code.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the client company creation request
     */
    public Response editClientCompany_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        boolean freshCreation = false;
        logger.info("authorise client company change, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        try {
            //get client company model from wrapper
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<ClientCompany> list = (List<ClientCompany>) wrapper.getModel();
            ClientCompany ccModel = list.get(0);
            
            boolean edited = cq.createOrUpdateClientCompany(retrieveClientCompanyModel(ccModel, freshCreation), retrieveAddressModel(ccModel, freshCreation), 
                    retrieveEmailAddressModel(ccModel, freshCreation), retrievePhoneNumberModel(ccModel, freshCreation));
            
            if (edited) {
                logger.info("client company edited - [{}]", ccModel.getName());
                resp.setRetn(0);
                resp.setDesc("Successful");
                return resp;
            }
            
            resp.setRetn(206);
            resp.setDesc("Unable to change client company from authorisation. Contact System Administrator");
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
            
            resp.setRetn(203);
            resp.setDesc("Unable to change client company from authorisation. Contact System Administrator");
            
            return resp;
        }
    }

    public Response queryClientCompany(Login login, QueryClientCompany queryParams) {
        Response resp = new Response();
        logger.info("query client company, invoked by [{}]", login.getUserId());
        
        Map<String, String> descriptors = Descriptor.decipherDescriptor(queryParams.getDescriptor());
        
        if (descriptors.size() == 4) {
            String descriptor = queryParams.getDescriptor();
            
            ClientCompany clientCompany;
            if (queryParams.getClientCompany() != null) {
                clientCompany = queryParams.getClientCompany();
            } else {
                clientCompany = new ClientCompany();
            }
            Address address;
            if (!queryParams.getClientCompany().getAddresses().isEmpty()) {
                address = queryParams.getClientCompany().getAddresses().get(0);
            } else {
                address = new Address();
            }
            EmailAddress emailAddress;
            if (!queryParams.getClientCompany().getEmailAddresses().isEmpty()) {
                emailAddress = queryParams.getClientCompany().getEmailAddresses().get(0);
            } else {
                emailAddress = new EmailAddress();
            }
            PhoneNumber phoneNumber;
            if (!queryParams.getClientCompany().getPhoneNumbers().isEmpty()) {
                phoneNumber = queryParams.getClientCompany().getPhoneNumbers().get(0);
            } else {
                phoneNumber = new PhoneNumber();
            }
            Map<String, Double> shareUnit;
            if (!queryParams.getShareUnit().isEmpty()) {
                shareUnit = queryParams.getShareUnit();
            } else {
                shareUnit = new HashMap<>();
            }
            Map<String, Integer> numberOfShareholders;
            if (queryParams.getNumberOfShareholders().isEmpty()) {
                numberOfShareholders = queryParams.getNumberOfShareholders();
            } else {
                numberOfShareholders = new HashMap<>();
            }
            Map<String, Integer> numberOfBondholders;
            if (queryParams.getNumberOfBondholders().isEmpty()) {
                numberOfBondholders = queryParams.getNumberOfBondholders();
            } else {
                numberOfBondholders = new HashMap<>();
            }

            //List<org.greenpole.hibernate.entity.ClientCompany> ccResult = cq.queryClientCompany(descriptor, clientCompany, address, phoneNumber, emailAddress, shareUnit, numberOfShareholders, numberOfBondholders)
        }
        
        return resp;
    }
    
    /**
     * Processes request to upload a list of share quotations.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param shareQuotation the list of share quotations to be uploaded
     * @return response to the share quotation upload request
     */
    public Response uploadShareUnitQuotations_Request(Login login, String authenticator, List<ShareQuotation> shareQuotation) {
        logger.info("request to upload share unit quotations from [{}]", login.getUserId());
        
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        
        //ensure all companies in share quotation list are legit
        int pos;
        boolean exists = false;
        for(pos = 0; pos < shareQuotation.size(); pos++) {
            exists = cq.checkClientCompany(shareQuotation.get(pos).getClientCompany().getCode());
            if (!exists)
                break; //if any one company code doesn't exist, break out of loop
        }
        
        //client company must exist before its share quotations can be uploaded
        if (exists) {
            wrapper = new NotificationWrapper();
            prop = new NotifierProperties(ClientCompanyComponentLogic.class);
            qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), 
                    prop.getAuthoriserNotifierQueueName());
            
            logger.info("client company codes exist");
            //wrap client company object in notification object, along with other information
            wrapper.setCode(Notification.createCode(login));
            wrapper.setDescription("Upload of share-unit quotations, requested by " + login.getUserId());
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(shareQuotation);
            
            resp = qSender.sendAuthorisationRequest(wrapper);
            logger.info("notification fowarded to queue, request by [{}] - notification code: [{}]", login.getUserId(), wrapper.getCode());
            return resp;
        }
        resp.setRetn(204);
        resp.setDesc("The client company code [" + shareQuotation.get(pos).getClientCompany().getCode() + "] does not exist.");
        logger.info("The client company code does not exist so cannot be edited - [{}]: [{}]", 
                shareQuotation.get(pos).getClientCompany().getCode(), resp.getRetn());
        return resp;
    }
    
    /**
     * Processes request to upload a list of share quotations that has already been saved.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the share quotation upload request
     */
    public Response uploadShareUnitQuotations_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("authorisation for share unit quotation upload, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<ShareQuotation> quotationList = (List<ShareQuotation>) wrapper.getModel();
            
            boolean uploaded = cq.uploadShareQuotation(retrieveShareQuotation(quotationList));
            
            if (uploaded) {
                logger.info("share unit quotation upload authorised");
                resp.setRetn(0);
                resp.setDesc("Successful");
                return resp;
            }
            
            resp.setRetn(205);
            resp.setDesc("Unable to change client company from authorisation. Contact System Administrator");
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
            
            resp.setRetn(203);
            resp.setDesc("Unable to change client company from authorisation. Contact System Administrator");
            
            return resp;
        }
    }

    /**
     * Unwraps the list share quotation models to create the list hibernate share quotation entities.
     * @param quotationList the list of share quotation models
     * @return the list hibernate share quotation entities
     */
    private List<org.greenpole.hibernate.entity.ShareQuotation> retrieveShareQuotation(List<ShareQuotation> quotationList) {
        List<org.greenpole.hibernate.entity.ShareQuotation> shareQuotations_hib = new ArrayList<>();
        org.greenpole.hibernate.entity.ShareQuotation shareQuotation = new org.greenpole.hibernate.entity.ShareQuotation();
        for (ShareQuotation sq : quotationList) {
            String code = sq.getClientCompany().getCode();
            org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(code);
            shareQuotation.setClientCompany(cc);
            shareQuotation.setUnitPrice(sq.getUnitPrice());
            shareQuotations_hib.add(shareQuotation);
        }
        return shareQuotations_hib;
    }
    
    /**
     * Unwraps the client company model to create the hibernate client company phone number entity.
     * @param ccModel the client company model (not to be confused with the client company hibernate entity)
     * @param freshCreation if the client company phone number is being created for the first time or undergoing an edit
     * @return a list of hibernate client company phone number entity object(s)
     */
    private List<org.greenpole.hibernate.entity.ClientCompanyPhoneNumber> retrievePhoneNumberModel(ClientCompany ccModel, boolean freshCreation) {
        org.greenpole.hibernate.entity.ClientCompanyPhoneNumber phone = new org.greenpole.hibernate.entity.ClientCompanyPhoneNumber();
        List<PhoneNumber> phoneList = ccModel.getPhoneNumbers();
        List<org.greenpole.hibernate.entity.ClientCompanyPhoneNumber> toSend = new ArrayList<>();
        
        for (PhoneNumber ph : phoneList) {
            ClientCompanyPhoneNumberId phoneId = new ClientCompanyPhoneNumberId();
            if (!freshCreation) {
                phoneId.setClientCompanyId(ph.getEntityId());
            }
            phoneId.setPhoneNumber(ph.getPhoneNumber());
            //put id in phone
            phone.setId(phoneId);
            //set other phone variables
            phone.setIsPrimary(ph.isPrimaryPhoneNumber());
            
            //put phone entity in list
            toSend.add(phone);
        }
        return toSend;
    }

    /**
     * Unwraps the client company model to create the hibernate client company email address entity.
     * @param ccModel the client company model (not to be confused with the client company hibernate entity)
     * @param freshCreation if the client company email address is being created for the first time or undergoing an edit
     * @return a list of hibernate client company email address entity object(s)
     */
    private List<org.greenpole.hibernate.entity.ClientCompanyEmailAddress> retrieveEmailAddressModel(ClientCompany ccModel, boolean freshCreation) {
        org.greenpole.hibernate.entity.ClientCompanyEmailAddress email = new org.greenpole.hibernate.entity.ClientCompanyEmailAddress();
        List<EmailAddress> emailList = ccModel.getEmailAddresses();
        List<org.greenpole.hibernate.entity.ClientCompanyEmailAddress> toSend = new ArrayList<>();
        
        for (EmailAddress em : emailList) {
            ClientCompanyEmailAddressId emailId = new ClientCompanyEmailAddressId();
            if (!freshCreation) {
                emailId.setClientCompanyId(em.getEntityId());
            }
            emailId.setEmailAddress(em.getEmailAddress());
            //put id in email
            email.setId(emailId);
            //set other email variables
            email.setIsPrimary(em.isPrimaryEmail());
            
            //add email to list
            toSend.add(email);
        }
        return toSend;
    }

    /**
     * Unwraps the client company model to create the hibernate client company address entity.
     * @param ccModel the client company model (not to be confused with the client company hibernate entity)
     * @param freshCreation if the client company address is being created for the first time or undergoing an edit
     * @return a list of hibernate client company address entity object(s)
     */
    private List<org.greenpole.hibernate.entity.ClientCompanyAddress> retrieveAddressModel(ClientCompany ccModel, boolean freshCreation) {
        org.greenpole.hibernate.entity.ClientCompanyAddress address = new org.greenpole.hibernate.entity.ClientCompanyAddress();
        List<Address> addressList = ccModel.getAddresses();
        List<org.greenpole.hibernate.entity.ClientCompanyAddress> toSend = new ArrayList<>();
        
        for (Address addy : addressList) {
            ClientCompanyAddressId addressId = new ClientCompanyAddressId();
            if (!freshCreation) {
                addressId.setClientCompanyId(addy.getEntityId());
            }
            addressId.setAddressLine1(addy.getAddressLine1());
            addressId.setState(addy.getState());
            addressId.setCountry(addy.getCountry());
            //put id in address
            address.setId(addressId);
            //set other address variables
            address.setIsPrimary(addy.isPrimaryAddress());
            address.setAddressLine2(addy.getAddressLine2());
            address.setAddressLine3(addy.getAddressLine3());
            address.setAddressLine4(addy.getAddressLine4());
            address.setCity(addy.getCity());
            address.setPostCode(addy.getPostCode());
            
            //add the address to list
            toSend.add(address);
        }
        return toSend;
    }

    /**
     * Unwraps the client company model to create the hibernate client company entity.
     * @param ccModel the client company model (not to be confused with the client company hibernate entity)
     * @param freshCreation if the client company is being created for the first time or undergoing an edit
     * @return the hibernate client company entity object
     */
    private org.greenpole.hibernate.entity.ClientCompany retrieveClientCompanyModel(ClientCompany ccModel, boolean freshCreation) {
        //instantiate required hibernate entities
        org.greenpole.hibernate.entity.ClientCompany cc_main = new org.greenpole.hibernate.entity.ClientCompany();
        Depository depository = new Depository();
        NseSector nseSector = new NseSector();
        //get values from client company model and insert into client company hibernate entity
        if (!freshCreation) {
            cc_main.setId(ccModel.getId());
        }
        cc_main.setName(ccModel.getName());
        cc_main.setCeo(ccModel.getCeo());
        cc_main.setCode(ccModel.getCode());
        cc_main.setSecretary(ccModel.getSecretary());
        //set depository id in object before setting object in hibernate client company object
        depository.setId(ccModel.getDepositoryId());
        cc_main.setDepository(depository);
        //set nse sector id in object before setting object in hibernate client company object
        nseSector.setId(ccModel.getNseSectorId());
        cc_main.setNseSector(nseSector);
        
        return cc_main;
    }
}
