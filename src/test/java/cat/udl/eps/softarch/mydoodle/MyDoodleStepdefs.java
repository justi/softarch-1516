package cat.udl.eps.softarch.mydoodle;

import cat.udl.eps.softarch.mydoodle.config.ApplicationConfig;
import cat.udl.eps.softarch.mydoodle.config.TestMailConfig;
import cat.udl.eps.softarch.mydoodle.model.*;
import cat.udl.eps.softarch.mydoodle.repository.MeetingProposalRepository;
import cat.udl.eps.softarch.mydoodle.repository.ParticipantAvailabilityRepository;
import cat.udl.eps.softarch.mydoodle.repository.TimeSlotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Created by http://rhizomik.net/~roberto/
 */

@WebAppConfiguration
@ContextConfiguration(classes = {ApplicationConfig.class, TestMailConfig.class})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class MyDoodleStepdefs {
    private static final Logger logger = LoggerFactory.getLogger(MyDoodleStepdefs.class);

    private String meetingURI;
    private String participantURI;
    private String timeSlotURI;
    private MockMvc       mockMvc;
    private ResultActions result;
    private MeetingProposal proposal;
    private UUID auxiliarId;
    private String adminKey;
    private String participantID;
    private String prova;

    @Autowired
    private WebApplicationContext wac;
    @Autowired
    private MeetingProposalRepository meetingRepos;
    @Autowired
    private TimeSlotRepository timeslotRepos;
    @Autowired
    private ParticipantAvailabilityRepository participantAvailabilityRepos;
    @Autowired
    public JavaMailSender javaMailSender;

    ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(this.wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @After
    public void tearDown() throws Exception {
    }

    @When("^the organizer creates a meeting proposal with title \"([^\"]*)\", description \"([^\"]*)\", organizer \"([^\"]*)\" and slot duration \"([^\"]*)\"$")
    public void the_organizer_creates_a_meeting_proposal_with_title_description_organizer_and_slot_duration(String title, String description, String orgEmail, int slotDuration) throws Throwable {
        result = mockMvc.perform(post("/meetingProposals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"title\": \"" + title + "\"" +
                        ", \"description\": \"" + description + "\"" +
                        ", \"organizer\": \"" + orgEmail + "\"" +
                        ", \"slotDuration\": " + slotDuration +
                        "}")
                .accept(MediaType.APPLICATION_JSON));

        meetingURI = result.andReturn().getResponse().getHeader("Location");
        try {
            adminKey = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.adminKey");
        } catch (PathNotFoundException e){
            adminKey = "";
        }
    }

    @Then("^the response is status code (\\d+)$")
    public void the_response_is_status_code(int statusCode) throws Throwable {
        result.andExpect(status().is(statusCode));
    }

    @And("^header \"([^\"]*)\" points to a proposal meeting with title \"([^\"]*)\", description \"([^\"]*)\", organizer \"([^\"]*)\"$")
    public void header_points_to_a_proposal_meeting_with_title_description_organizer(String header, String title, String description, String organizer) throws Throwable {
        result = mockMvc.perform(get(meetingURI + "?key=" + adminKey).accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title", is(title)))
                .andExpect(jsonPath("$.description", is(description)))
                .andExpect(jsonPath("$.organizer", is(organizer)));
    }

    @And("^header \"([^\"]*)\" points to a proposal meeting which has isOpen \"([^\"]*)\"$")
    public void header_points_to_a_proposal_meeting_which_has_isOpen(String header, boolean isOpen) throws Throwable {
        result = mockMvc.perform(get(meetingURI + "?key=" + adminKey).accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.isOpen", is(isOpen)));
    }

    @And("^header \"([^\"]*)\" points to a proposal meeting which has a \"([^\"]*)\" list of \"([^\"]*)\" containing \"([^\"]*)\" elements")
    public void header_points_to_a_proposal_meeting_with_a_list_of_with(String header, String fieldName, String type, int numElements) throws Throwable {
        result = mockMvc.perform(get(meetingURI+"/"+fieldName).accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$._embedded.[?(@=~/" + type + "/i)]", hasSize(numElements)));
    }

    @And("^error message contains \"([^\"]*)\"$")
    public void error_message_contains(String message) throws Throwable {
        result.andExpect(jsonPath("$.message", containsString(message)));
    }

    @And("^adds a participant with \"([^\"]*)\" email to the previously created meeting proposal$")
    public void adds_a_participant_with_email(String email) throws Throwable {
        result = mockMvc.perform(post("/participantAvailabilities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"participant\": \"" + email + "\" }")
                .accept(MediaType.APPLICATION_JSON));

        participantURI = result.andReturn().getResponse().getHeader("Location");

        int indexPart = participantURI.lastIndexOf("/");
        participantID= participantURI.substring(indexPart + 1, participantURI.length());

        result = mockMvc.perform(put(participantURI + "/meeting")
                .contentType("text/uri-list")
                .content(meetingURI)
                .accept(MediaType.APPLICATION_JSON));
    }

    @And("^adds a participant without email$")
    public void adds_a_participant_without_email() throws Throwable {
        result = mockMvc.perform(post("/participantAvailabilities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"participant\": \"" + "\" }")
                .accept(MediaType.APPLICATION_JSON));
    }

    @Then("^the participant has the email \"([^\"]*)\" and meeting associated$")
    public void the_participant_has_the_email_and_meeting_associated(String participant) throws Throwable {
        result = mockMvc.perform(get(participantURI).
                accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.participant", is(participant)));

        result = mockMvc.perform(get(participantURI+"/meeting").
                accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$._links.self.href", is(meetingURI)));
    }

    @When("^the organizer has created a meeting proposal with title \"([^\"]*)\", description \"([^\"]*)\", organizer \"([^\"]*)\" and slot duration \"([^\"]*)\"$")
    public void the_organizer_has_created_a_meeting_proposal_with_title_description_organizer_and_slot_duration(String Title, String Description, String email, String timeSlot) throws Throwable {
        this.proposal = new MeetingProposal(Title,Description,email,Integer.parseInt(timeSlot));
    }

    @And("^has added participant with \"([^\"]*)\" email$")
    public void has_added_participant_with_email(String email) throws Throwable {
        ParticipantAvailability participant = new ParticipantAvailability();
        participant.setParticipant(email);
        this.proposal.getAvailabilities().add(participant);
    }

    @When("^the organizer creates the meeting proposal:$")
    public void the_organizer_creates_a_meeting_proposal_with_title_description_organizer_and_slot_duration(List<MeetingProposal> meetingProposals) throws Throwable {
        result = mockMvc.perform(post("/meetingProposals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(meetingProposals.get(0)))
                .accept(MediaType.APPLICATION_JSON));

        proposal = meetingProposals.get(0);
        meetingURI = result.andReturn().getResponse().getHeader("Location");
        try {
            adminKey = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.adminKey");
        } catch (PathNotFoundException e){
            adminKey = "";
        }
    }

    @When("^the organizer creates a new time slot \"([^\"]*)\"$")
    public void the_organizer_creates_a_new_time_slot(String date) throws Throwable {
        result=mockMvc.perform(post("/timeSlots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"dateTime\": \"" + date + "\" }")
                .accept(MediaType.APPLICATION_JSON));

        timeSlotURI = result.andReturn().getResponse().getHeader("Location");
    }

    @When("^the organizer associates the previous time slot to the created meeting proposal$")
    public void the_organizer_associates_the_time_slot_to_the_created_meeting_proposal() throws Throwable {
        result = mockMvc.perform(put(timeSlotURI + "/meeting")
                .contentType("text/uri-list")
                .content(meetingURI)
                .accept(MediaType.APPLICATION_JSON));
    }

    @And("^the time slot has time \"([^\"]*)\" and the last created meeting associated$")
    public void the_time_slot_has_time_and_the_previous_meeting_associated(String date) throws Throwable {
        result = mockMvc.perform(get(timeSlotURI).
                accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dateTime", is(date)));

        result = mockMvc.perform(get(timeSlotURI+"/meeting").
                accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$._links.self.href", is(meetingURI)));
    }

    @Given("^the meetingsProposal repository has the following meetingProposals:$")
    public void the_MeetingProposals_repository_has_the_following_MeetingProposals(List<MeetingProposal> MeetingProposals) throws Throwable {
        for (MeetingProposal g: MeetingProposals)
            meetingRepos.save(g);
    }

    @When("^the participant views a \"([^\"]*)\" meeting proposal$")
    public void the_participant_view_a_existing_meeting_proposal_with_correct_acces(String typeId) throws Throwable {
        UUID id = (typeId.equals("existent")) ? meetingRepos.findAll().iterator().next().getId() : auxiliarId;
        result = mockMvc.perform(get("/meetingProposals/" + id.toString() + "?key=" + adminKey).accept(MediaType.APPLICATION_JSON));
    }

    @Then("^the response is a meetingProposal with title \"([^\"]*)\"$")
    public void response_is_meeting_proposal_with_title_content(String title) throws Throwable{
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value(is(title)));
    }

    @Given("^meetingProposal with random id doesn't exist$")
    public void meeting_proposal_random_not_exists() throws Throwable {
        auxiliarId = UUID.randomUUID();
        assertFalse(meetingRepos.exists(auxiliarId));
    }

    @When("^the participant views a \"([^\"]*)\" meeting proposal with \"([^\"]*)\" time slots$")
    public void the_participant_view_a_existing_meeting_proposal_with_correct_acces_and_timeSlots(String typeId, int numTimeSlots) throws Throwable {
        UUID id = (typeId.equals("existent")) ? meetingRepos.findAll().iterator().next().getId() : auxiliarId;
        for(int i=0; i<numTimeSlots; i++){
            ZonedDateTime now = ZonedDateTime.now();
            String date = now.toString().substring(0, now.toString().indexOf("["));
            add_a_new_time_slot(date, id.toString());
        }
        result = mockMvc.perform(get("/meetingProposals/{id}/slots", id.toString()).accept(MediaType.APPLICATION_JSON));
    }
    private void add_a_new_time_slot(String date, String id) throws Throwable {
        result=mockMvc.perform(post("/timeSlots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"dateTime\": \"" + date + "\" " +
                        ", \"meeting\": \"" + "meetingProposals/" + id + "\" }")
                .accept(MediaType.APPLICATION_JSON));
    }

    @Then("^the response is a meetingProposal with \"([^\"]*)\" time slots$")
    public void the_response_is_a_meetingProposal_with_time_slots(int numTimeSlots) throws Throwable {
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$._embedded.timeSlots", hasSize(numTimeSlots)));
    }

    @When("^the participant views a \"([^\"]*)\" meeting proposal with email participant \"([^\"]*)\"$")
    public void add_a_new_participant_with_email(String typeId, String email) throws Throwable {
        UUID id = (typeId.equals("existent")) ? meetingRepos.findAll().iterator().next().getId() : auxiliarId;
        result = mockMvc.perform(post("/participantAvailabilities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"participant\": \"" + email + "\"" +
                        ", \"meeting\": \"" + "meetingProposals/" + id + "\"" +
                        "}")
                .accept(MediaType.APPLICATION_JSON));

        result = mockMvc.perform(get("/meetingProposals/{id}/availabilities", id.toString()).accept(MediaType.APPLICATION_JSON));
    }

    @Then("^we will see (\\d+) participants$")
    public void we_will_see_participants(int participants) throws Throwable {
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$._embedded.participantAvailabilities", hasSize(participants)));
    }

    @And("^participant use \"([^\"]*)\" as admin key$")
    public void use_as_admin_key(String key) throws Throwable {
        adminKey = key;
    }

    @When("^the organizer updates the meeting title to \"([^\"]*)\"$")
    public void the_organizer_updates_the_meeting_title_to(String newValue) throws Throwable {
        proposal.setTitle(newValue);
        result = mockMvc.perform(put(meetingURI + "?key=" + adminKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(proposal))
                .accept(MediaType.APPLICATION_JSON));
    }

    @When("^the organizer deletes the meeting proposal$")
    public void the_organizer_deletes_the_meeting_proposal() throws Throwable {
        result =  mockMvc.perform(delete(meetingURI + "?key=" + adminKey));
    }

    @And("^meeting proposal repository is empty$")
    public void meeting_proposal_repository_is_empty() throws Throwable {
        assertEquals(meetingRepos.count(), 0);
    }

    @And("^the meeting has one list of size (\\d+)$")
    public void the_meeting_has_one_list_of_size(int num) throws Throwable {
        result = mockMvc.perform(get(meetingURI+"/availabilities").accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$._embedded.participantAvailabilities", hasSize(num)));
    }

    @Then("^the response is ok$")
    public void the_response_is_ok() throws Throwable {
        // Express the Regexp above with the code you wish you had
        result = mockMvc.perform(get(meetingURI).accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk());
    }

    @And("^The organizer create a new availability \"([^\"]*)\"$")
    public void The_organizer_create_a_new_availability(String availability) throws Throwable {
        result=mockMvc.perform(post("/timeSlotAvailabilities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"availability\": \"" + availability + "\" }")
                .accept(MediaType.APPLICATION_JSON));
    }

    @And("^the organizer associates the previous meeting proposal with the email participant, timeslot and availability \"([^\"]*)\"$")
    public void the_organizer_associates_the_previous_meeting_proposal_with_the_email_participant_timeslot_and_availability(String availability) throws Throwable {
        int indexTimeSlot = timeSlotURI.lastIndexOf("/");
        String idTimeSlot = timeSlotURI.substring(indexTimeSlot+1,timeSlotURI.length());
       /* int indexPart = participantURI.lastIndexOf("/");
        String idParticipant = participantURI.substring(indexPart+1,participantURI.length());*/
        result=mockMvc.perform(post("/timeSlotAvailabilities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"participant\": \"" + "participantAvailabilities/" + participantID + "\" " +
                        ", \"timeSlot\": \"" + "timeSlots/" + idTimeSlot + "\" " +
                        ", \"availability\": \"" + availability + "\" }")
                .accept(MediaType.APPLICATION_JSON));

        prova = result.andReturn().getResponse().getHeader("Location");
    }

   @And("^there is (\\d+) slots availabilities in participant availability$")
    public void there_is_slots_availabilities_in_participant_availability(int slots) throws Throwable {
        result = mockMvc.perform(get(participantURI+"/slotAvailabilities").accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$._embedded.timeSlotAvailabilities", hasSize(slots)));
    }


    @And("^there is (\\d+) slots availabilities and (\\d+) participant in meeting proposal, none or single participant$")
    public void there_is_slots_availabilities_in_participant_availability_none_single(int slots, int participant) throws Throwable {
        result = mockMvc.perform(get(meetingURI+"/availabilities").accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$._embedded.participantAvailabilities[0].slotsAvailabilities", hasSize(slots)))
                .andExpect(jsonPath("$._embedded.participantAvailabilities", hasSize(participant)));
    }

    @And("^there is (\\d+) slots availabilities and (\\d+) participant in meeting proposal, two participants$")
    public void there_is_slots_availabilities_in_participant_availability_two(int slots, int participant) throws Throwable {
        result = mockMvc.perform(get(meetingURI+"/availabilities").accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$._embedded.participantAvailabilities[0].slotsAvailabilities", hasSize(slots)))
                .andExpect(jsonPath("$._embedded.participantAvailabilities[1].slotsAvailabilities", hasSize(slots)))
                .andExpect(jsonPath("$._embedded.participantAvailabilities", hasSize(participant)));
    }

    @And("^the organizer adds time slot \"([^\"]*)\" to the meeting proposal \"([^\"]*)\"$")
    public void the_organizer_adds_time_slot_to_the_meeting_proposal(String dateTime, String meetingTitle) throws Throwable {
        MeetingProposal meeting = meetingRepos.findByTitle(meetingTitle).get(0);

        result = mockMvc.perform(post("/timeSlots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"dateTime\": \"" + dateTime + "\" " +
                        ", \"meeting\": \"" + "meetingProposals/" + meeting.getId() + "\" }")
                .accept(MediaType.APPLICATION_JSON));
    }

    @And("^the organizer invites participant \"([^\"]*)\" to the meeting proposal \"([^\"]*)\"$")
    public void the_organizer_invites_participant_to_the_meeting_proposal(String participantEmail, String meetingTitle) throws Throwable {
        MeetingProposal meeting = meetingRepos.findByTitle(meetingTitle).get(0);

        result = mockMvc.perform(post("/participantAvailabilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"participant\": \"" + participantEmail + "\"" +
                                ", \"meeting\": \"" + "meetingProposals/" + meeting.getId() + "\"" + "}")
                        .accept(MediaType.APPLICATION_JSON));
    }


    @And("^the participant \"([^\"]*)\" sets availability \"([^\"]*)\" for time slot \"([^\"]*)\"$")
    public void the_participant_sets_availability_for_time_slot(String participantEmail, Availability availability, String dateTime) throws Throwable {
        ParticipantAvailability participant = participantAvailabilityRepos.findByParticipant(participantEmail).get(0);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
        TimeSlot timeSlot = timeslotRepos.findByDateTime(df.parse(dateTime)).get(0);

        result = mockMvc.perform(post("/timeSlotAvailabilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"participant\": \"" + "participantAvailabilities/" + participant.getId() + "\" " +
                                 ", \"timeSlot\": \"" + "timeSlots/" + timeSlot.getId() + "\" " +
                                 ", \"availability\": \"" + availability + "\" }")
                        .accept(MediaType.APPLICATION_JSON));
    }

    @When("^the organizer schedules time slot \"([^\"]*)\" and closes the meeting proposal \"([^\"]*)\"$")
    public void the_organizer_schedules_time_slot_and_closes_the_meeting_proposal(String dateTime, String meetingTitle) throws Throwable {
        MeetingProposal meeting = meetingRepos.findByTitle(meetingTitle).get(0);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
        TimeSlot timeSlot = timeslotRepos.findByDateTime(df.parse(dateTime)).get(0);

        result = mockMvc.perform(patch("/meetingProposals/" + meeting.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"isOpen\": \"False\" }"));

        result = mockMvc.perform(patch("/meetingProposals/" + meeting.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"schedule\": \"" + "timeSlots/" + timeSlot.getId() + "\" }"));
    }

    @Then("^an email has been sent to \"([^\"]*)\" containing \"([^\"]*)\"$")
    public void an_email_has_been_sent_to_containing(String recipient, String text) throws Throwable {
        ArgumentCaptor<SimpleMailMessage> argument = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, atLeastOnce()).send(argument.capture());
        SimpleMailMessage lastEMail = argument.getAllValues().get(argument.getAllValues().size()-1);
        assertTrue(lastEMail.getTo()[0].equals(recipient));
        assertThat(lastEMail.getText(), containsString(text));
    }

    @And("^an email has been sent to \"([^\"]*)\" and \"([^\"]*)\" containing \"([^\"]*)\"$")
    public void an_email_has_been_sent_to_and_containing(String recipient1, String recipient2, String text) throws Throwable {
        ArgumentCaptor<SimpleMailMessage> argument = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, atLeastOnce()).send(argument.capture());
        SimpleMailMessage lastEMail = argument.getAllValues().get(argument.getAllValues().size()-1);
        assertTrue(lastEMail.getTo()[0].equals(recipient2));
        assertThat(lastEMail.getText(), containsString(text));
        SimpleMailMessage penultimEMail = argument.getAllValues().get(argument.getAllValues().size()-2);
        assertTrue(penultimEMail.getTo()[0].equals(recipient1));
        assertThat(penultimEMail.getText(), containsString(text));
    }

    @Then("^the organizer puts isOpen as \"([^\"]*)\"$")
    public void the_organizer_puts_isOpen_as(boolean isOpen) throws Throwable {
        proposal.setIsOpen(isOpen);
        result = mockMvc.perform(put(meetingURI + "?key=" + adminKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(proposal))
                .accept(MediaType.APPLICATION_JSON));
    }

    @And("^the votes for \"([^\"]*)\" are (\\d+)$")
    public void the_votes_for_are(String availability, int votes) throws Throwable {
        result = mockMvc.perform(get(timeSlotURI).accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$."+availability+"Votes", is(votes)));
    }

    @Then("^the organizer sets one timeSlot as the schedule$")
    public void the_organizer_sets_the_last_created_timeSlot_as_the_schedule() throws Throwable {
        result=mockMvc.perform(put(timeSlotURI)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"schedule\": \"" + true + "\" }")
                .accept(MediaType.APPLICATION_JSON));

        result = mockMvc.perform(get(timeSlotURI).accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.schedule", is(true)));
    }

    @And("^The organizer deletes the participant with \"([^\"]*)\" email.$")
    public void The_organizer_deletes_the_participant_with_email(String email) throws Throwable {
        // Express the Regexp above with the code you wish you had
        // throw new PendingException();
        result = mockMvc.perform(delete(participantURI));

        participantURI = result.andReturn().getResponse().getHeader("Location");


        //result = mockMvc.perform(get(participantURI));


    }
}
