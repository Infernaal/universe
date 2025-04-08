*** Settings ***
Library           SeleniumLibrary
Suite Setup       Open Browser To Start Page
Suite Teardown    Close Browser

*** Variables ***
${URL}            https://127.0.0.1:7000

*** Test Cases ***
Open Homepage And Check Title
    Title Should Be    Universe

Check Header Is Present
    Page Should Contain    Welcome to the Universe

Check Start Button Is Present
    Page Should Contain Button    Start Exploring

Click Start Button And Check Transition
    Click Button    Start Exploring
    Wait Until Page Contains    Explore the Stars    5s

Check Footer Text
    Page Should Contain    © Universe 2024

Resize Browser For Responsiveness
    Set Window Size    375    812
    Page Should Contain    Menu
    Set Window Size    1920    1080

Open Invalid Page And Verify Error
    Go To    ${URL}/non-existent-page
    Page Should Contain    404

Navigation Menu Should Exist
    Page Should Contain Element    xpath=//nav

Check Presence Of Images
    Page Should Contain Element    xpath=//img
