[![Release version](https://img.shields.io/github/v/release/mars-sim/mars-sim?sort=semver&color=blue&label=release&style=flat-square)](https://github.com/mars-sim/mars-sim/releases/latest)
[![Repo Size](https://img.shields.io/github/repo-size/mars-sim/mars-sim?style=flat-square)](https://github.com/mars-sim/mars-sim/releases/latest)
[![Commits Since](https://img.shields.io/github/commits-since/mars-sim/mars-sim/v3.6.1?sort=semver)](https://github.com/mars-sim/mars-sim/commits/v.3.6.1)
[![Commits Since](https://img.shields.io/github/commits-since/mars-sim/mars-sim/v3.6.0?sort=semver)](https://github.com/mars-sim/mars-sim/commits/v3.6.0)
[![Last Commit](https://img.shields.io/github/last-commit/mars-sim/mars-sim?style=flat-square)](https://github.com/mars-sim/mars-sim/commits)
[![GitHub Downloads](https://img.shields.io/github/downloads/mars-sim/mars-sim/total?label=gitHub%20downloads&style=flat-square&color=blue)](https://github.com/mars-sim/mars-sim/releases)

[![Gitter](https://badges.gitter.im/mokun/mars-sim.svg)](https://gitter.im/mokun/mars-sim?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=com.github.mars-sim%3Amars-sim&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=com.github.mars-sim%3Amars-sim)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/mars-sim/mars-sim.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/mars-sim/mars-sim/alerts/)
[![License](https://img.shields.io/badge/license-GPL%203.0-blue.svg)](http://www.gnu.org/licenses/gpl-3.0.html)
[![Language](http://img.shields.io/badge/language-java-brightgreen.svg)](https://www.java.com/)
[![SF Monthly Download](https://img.shields.io/sourceforge/dm/mars-sim.svg?label=sf%20download&style=flat-square)](https://sourceforge.net/projects/mars-sim/files/mars-sim/)


# $$\color{#D29922}\textsf{\kern{0.2cm}\normalsize mars-sim}$$ 

## Table of Contents
* [Introduction](#introduction)  
   * [Simulation](#simulation)
   * [Exploration](#exploration)
   * [Mars Direct Mission Plan](mars-direct-mission-plan) 
   * [Settlement](#settlement)
   * [Economics](#economics)   
   * [Countries](#countries)   
* [Operation Modeling](#operation-modeling)
   * [Timekeeping](#timekeeping)
   * [Indoor Atmosphere](#indoor-atmosphere)
   * [EVA](#eva)
   * [Radiation](#radiation)
   * [Job](#job)
   * [Role](#role)
   * [Task](#task)
   * [Work Shift](#work-shift)
   * [Mission](#mission)
   * [Weather](#weather)
   * [Maintenance and Malfunction](#maintenance-and-malfunction)
* [Summary](#summary)
* [Set up](#set-up)
   * [Prerequisites](#prerequisites)
   * [JDK and JavaFX](#jdk-and-javafx)
   * [OS Platforms](#os-platforms)
   * [Remote Console Connection](#remote-console-connection)
* [Outreach](#outreach)
* [Discussions](#discussions)
* [Issues and Tickets](#issues-and-tickets)
* [Contribution](#contribution)
* [Website](#website)
* [Wiki](#wiki)
* [Supported Platforms](#supported-platforms)
* [Official Codebase](#official-codebase)
* [Download](#download)
* [License](#license)

---

# Introduction
*The Mars Simulation Project* is a Java-based [open source](https://opensource.dev/) project that
simulates mission operations and activities of initial settlers on Mars with a higher fidelity of 
modeling and simulation details than a classic PC game of its genre.

## Simulation
mars-sim is designed to be a **general purpose** simulator depicting the early development of human settlements on Mars.

According to *Encyclopædia Britannica*, a computer simulation is the use of a computer to represent
the dynamic responses of one system by the behavior of another system modeled after it. In essence,
a simulation is a mathematical description, or model, of a real system in the form of a computer program.

mars-sim aims to integrate and incorporate as many research disciplines (such as physics, chemistry,
biology, economics, psychology, and social science) as possible to simulate the dynamics and behaviors
of people, social structure, physical and mechanical systems, and environment in the context of
developing human settlements on Mars.

mars-sim models each settler as an intelligent agent which possess varying degrees of autonomy and mobility.
It is a symbolic model of reality, given a capacity to learn from experiences and an ability to
cooperate with other agents and systems. A settler has prescribed attributes and skills and makes
weighted decisions and interacts with one another to produce unexpected results in a sandbox world.

For instance, each settler is assigned with a job, a role, having technical
[skills](https://github.com/mars-sim/mars-sim/wiki/Skills), [personality traits](https://github.com/mars-sim/mars-sim/wiki/Personality), natural
[attributes](https://github.com/mars-sim/mars-sim/wiki/Attributes), and preferences of tasks.
They build [relationship](https://github.com/mars-sim/mars-sim/wiki/Relationship)
as they interact and work with one another. They are there to live, dream, explore, and settle Mars.

## Exploration

History is shaped by pioneers. The exploration of Mars started in the 1960s with sending robotic 
spacecraft to orbit the planet. The apex of this spirit of exploration will be culminated by the first human landing 
in near future. Early explorers of Mars will come face-to-face with insurmountable challenges. 
The first generations human explorers will not explore long distances from their habitat but within 
a region being called an "Exploration Zone", say, within 100 km of their landing. Many mission planning
activities would have been taken place prior to their arrival to first identify all the Science and Resource 
Region of Interests (ROI). mars-sim is modeled to simulate the exploration of Mars within a decade after 
the first landing. Humanity has shifted into high gears with the effort of making Mars the second home.

## Mars Direct Mission Plan 

mars-sim loosely follows the *Mars Direct Mission Plan by Robert Zubrin* and has crafted 5 basic
settlement types, namely, Phase 1 Base, Phase 2 Base, Phase 3 Base, Alpha Base and special outposts. 

In general, a 4-settler initial base is called a *Mars Direct Plan (MDP) Phase 1* template. An 8-settler base
is constructed under *MDP Phase 2* template. A 12-settler base is *MDP Phase 3*. A 24-settler base
is designated as *Alpha Base*. Besides, players may build a *Trading Outpost* or a *Mining Depot*
near sites with high mineral concentrations. 

Depending on its country or origin and/or sponsor, each template may vary in the numbers and types of building it contains.
Altogether, there is a total of 24 [settlement templates](https://github.com/mars-sim/mars-sim/wiki/Settlement-Templates) 
to choose from.

## Settlement

The selection of a landing site is determined by a variety of factors. 
One of the goals of mars-sim is to populate Mars with human settlements.
Each settlement has an initial sponsor to guide its development but will eventually develop
its own *[command structure](https://github.com/mars-sim/mars-sim/wiki/Role)* and
*[development objective](https://github.com/mars-sim/mars-sim/wiki/Settlement-Objective)*.

## Economics

In terms of [economics](https://github.com/mars-sim/mars-sim/wiki/Economics) modeling, mars-sim implements the
**Value Point (VP)** system, which keeps track of the supply and demand on each good and resource.
As there is no standard currency established on Mars yet, settlers barter trades with neighboring settlements
by keeping track of the credits and deficit based on the VPs of the resources in exchange in each trading session.

## Countries 

It is an incredible undertaking in that 30 [countries](https://github.com/mars-sim/mars-sim/wiki/Countries) 
are participating in this dream of making Mars as the second home for humanity. Altogether, there's a total of 9 
possible space agencies to choose from as a sponsor to start a settlement. Notably, European Space Agency (ESA) 
is a bloc that consists of 22 member nations willing to fund this space development effort. 

---

# Operation Modeling

mars-sim depicts near-term human exploration and settlement on Mars. It speaks of a rich scientific
language selected from research journal and paper in defining operation paradigms and programming models
that are based on present-day technologies.

## Timekeeping

Without a doubt, settlers need a timekeeping standard system for tracking the passage of time. 
That's because living on Mars does require a functioning Martian calendar in which settlers may keep track 
of days (or sols) that have elapsed. At the same time, astronomers would prefer to come up with a calendar that 
is handy and intuitive in predicting the orbit of Mars around the sun. 

The difficulties arises when each sol on Mars has slightly more than 24 earth hours and there are 669 earth days 
on one Martian orbit (or year). Therefore, it is not a straight-foward exercise in converting the time and day 
between Mars and Earth by merely a simple equation.

See [timekeeping wiki](https://github.com/mars-sim/mars-sim/wiki/Timekeeping) for further discussions on 
this topic.

## Indoor Atmosphere

While at the Earth's sea level, the atmospheric pressure is **101.35 kPa** (14.7 psi) and has 20.9% oxygen,
in mars-sim, a low pressure atmosphere of **34 kPa** (4.93 psi) is chosen for the settlement living with
the composition of oxygen at 58.8%. 

However, [EVA suit](https://github.com/mars-sim/mars-sim/wiki/EVA-Suit) or rovers (inside a vehicle) 
adopt an even lower pressurized environment of 17 kPa (2.47 psi) for more optimal use of resources 
and design specifications. In comparison, Apollo Lunar Module (LM) atmosphere of 100% oxygen at 33 kPa
(4.8 psi). The National Aeronautics and Space Administration (NASA)'s Shuttle airlock has an oxygen 
concentration of 30% at 70.3 kPa (10.2 psi). NASA's Extravehicular Mobility Units (EMU) has the 
operating pressure of 29.6 kPa (4.3 psi). The upcoming Artemis program's lunar lander will have an atmosphere 
of 342% oxygen at a pressure of 56.5 kPa (8.2 psi).

See [Atmosphere](https://github.com/mars-sim/mars-sim/wiki/Atmosphere) wiki for more design details.

In mars-sim, each habitable building has a life-support system with various 
[functions](https://github.com/mars-sim/mars-sim/wiki/Building-Function) built-in
that continuously monitor and periodically replenish oxygen, carbon dioxide, and water moisture.
These gases are produced via chemical systems such as **Sabatier Reverse Water Gas (SRWG)**, and
**Oxygen Generation System (OGS)**, etc.

Structurally speaking, a low-pressure environment reduces the need for a rigid structure that supports
various load requirements for a building. It also facilitates occupants' Extra-Vehicular Activity (EVA)
with the outside world.

## EVA

An example of operation modeling is the sequence of steps involving the ingress and egress of airlocks.

To walk onto the surface of Mars, a settler must come through an intermediate chamber
called the *airlock* to exit the settlement. The airlock allows the passage of people between
a pressure vessel and its surroundings while minimizing the change of pressure in the vessel and loss of
air from it. 

In mars-sim, the airlock is a separate building joined to any *Hab* (which stands for cylindrical
*habitation module*) such as *Lander Hab*, or *Outpost Hub*, *Astronomy Observatory*, etc.
All rovers have vehicular airlock built-in.

To perform a team EVA, one of the members will be selected as the *airlock operator*, who will ensure that proper
procedures be followed before going out for an EVA or after coming back from an EVA.

In case of an egress operation, 
(1) the airlock would have to be *pressurized*. 
(2) The air would be heated
so that the atmospheric pressure and temperature are equalized. 
(3) Next, the airlock operator would unlock and open the inner door. 
(4) The whole team would enter into the airlock. 
(5) After all have donned EVA suits, the operator will depressurize the chamber and gases would be re-captured to match the
outside air pressure. 
(6) At last, he/she would unlock and open the outer door and the whole team will exit to the outside surface of Mars.

See [Airlock wiki](https://github.com/mars-sim/mars-sim/wiki/Airlock) for details on this topic.

## Radiation

Another example is [Radiation Modeling](https://github.com/mars-sim/mars-sim/wiki/Radiation-Exposure),
which account for how often the **Galactic Cosmic Ray (GCR)** and **Solar Energetic Particles (SEP)**
would occur during EVA. The cumulative dose is closely monitored in 3 specific exposure interval,
namely, the 30-day, the annual and the career lifetime of a settler. It would affect 3 different regions
of our body, namely, the *Blood Forming Organs (BFO)*, the *Ocular Lens*, and the *Skin*. The dose limits are
measured in *milli-Severt*.

## Job

Each settler is initially assigned a meaningful [job](https://github.com/mars-sim/mars-sim/wiki/Jobs) 
that fit one's attributes and career profile. Player may designate the job of a settler in the xml file.

## Role

Each settlement has a command structure that brings each settler a [role](https://github.com/mars-sim/mars-sim/wiki/Role) 
to play. 

## Task

Settlers spend much of their time learning to *live off the land* and engage in various 
[tasks](https://github.com/mars-sim/mars-sim/wiki/Tasks) such as maintenance, 
ensuring life support resources are plentifully supplied, growing food crops in
[greenhouses](https://github.com/mars-sim/mars-sim/wiki/Greenhouse-Operation), making secondary
[food products](https://github.com/mars-sim/mars-sim/wiki/Food-Production), and manufacturing needed parts
and equipment in workshops, all of which are vital to the health of the economy of the settlements. 

## Work Shift

Each settler is assigned a [work shift](https://github.com/mars-sim/mars-sim/wiki/Work-Shift) during each sol.
The duration of a work shift may be one third of a sol or a quarter of a sol.

## Mission

Settlers also go out on field [Missions](https://github.com/mars-sim/mars-sim/wiki/Missions) to explore and
study the surrounding landscapes, to prospect and mine minerals, and to trade with neighboring settlements, etc.
They may even decide to migrate from one settlement to another.

## Weather

The perils of living on Mars are very real. Even though we do not have a complete surface weather model for Mars,
we do currently simulate a total of 9 outside [weather metrics](https://github.com/mars-sim/mars-sim/wiki/Weather)
in mars-sim. 

## Maintenance and Malfunction

The perils of living on Mars are very real. There is a total of 39 types of 
[Malfunctions](https://github.com/mars-sim/mars-sim/wiki/Malfunctions) that can occur at a given moment. 

There are 3 metrics for tracking how reliable a Part is. The [Reliability](https://github.com/mars-sim/mars-sim/wiki/Reliability)
is shown in terms of Percentage, Failure Rate, Mean Time Between Failure (MTBF), which are 
dynamically updated in light of any incidents that occur during the simulation. Besides 
malfunction, workshops and machinery factories are to produce parts for
replenishing parts to be used during regular [maintenance](https://github.com/mars-sim/mars-sim/wiki/Maintenance) tasks.


---

## Summary
Mars is a harsh world but is certainly less unforgiving than our Moon. Settlers come face-to-face with accidents,
equipment malfunctions, illnesses, injuries, and even death. Survival depends on how well they work together,
improve their survival skills and balance individual versus settlement needs.

As the settlers learn how to survive hardship and build up their settlements, players are rewarded with the
pure joy of participating in this grand social experiment of creating a new branch of human society on another
planetary surface.

---

# Set up

Below is a summary of how player may set up one's machine to evaluate and develop mars-sim

## Prerequisites

<a href="https://foojay.io/today/works-with-openjdk">
   <img align="right"
        src="https://github.com/foojayio/badges/raw/main/works_with_openjdk/Works-with-OpenJDK.png"
        width="100">
</a>

Currently, mars-sim supports Java 17 which is the latest long-term support (LTS) release.

* Requires only JRE 17 for running mars-sim
* Requires only JDK 17 (or openjdk 17) for compiling binary

## JDK and JavaFX

Beginning Java 11, the JRE/JDK package is being decoupled from the graphic
JavaFX API package.

For the open source community, the OpenJDK is also being decoupled from the OpenJFX.

Currently, mars-sim does not require JavaFX.

> Note 1 : Specifically, the official release of mars-sim (v3.1.0 to v3.5.0) do not
utilize JavaFX / OpenJFX. 

Therefore, it's NOT a requirement to install it for running mars-sim.

Some unofficial releases of mars-sim in the past may have required JavaFX.

However, if you want to run any other JavaFX apps, make sure you download and
configure the OpenJFX or JavaFX package on top of the JDK.

See ticket #156 to read the discussions on how to set up JavaFX to run it
under Java 11.

Obtain the latest JRE/JDK for your platform. Here are some of the popular OpenJDK packages out there :

* [Amazon Cornetto](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)

* [Microsoft](https://learn.microsoft.com/en-us/java/openjdk/download)

* [Liberica](https://bell-sw.com/pages/downloads/)

* [OpenLogic](https://www.openlogic.com/openjdk-downloads)

If you need JavaFX, we recommend downloading the `Full JDK` 64-bits package.

In case of Liberica, the `Full JDK` includes LibericaFX, which is based on OpenJFX, for
running other apps that requires JavaFX.


## OS Platforms

Assuming that OpenJDK 17 is being used.

### Linux

1. The debian edition of mars-sim comes with debian installer for quick installation. However,
you will have to install, configure and properly update the JDK or openJDK binary in your linux
 machine in order to run mars-sim. Please google to find out the most updated instructions for your distro.

2. To manage multiple versions of java with the use of a tool called `SDKMan`,
see this [DZone article](https://dzone.com/articles/how-to-install-multiple-versions-of-java-on-the-sa).

### macOS

1.  Check if the directory of JDK is at `Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home`.
See [DZone](https://dzone.com/articles/installing-openjdk-11-on-macos) for more instructions.

2. Enter `/usr/libexec/java_home -V` to find out which version of Java you have installed.

### Windows

Choose MSI version that will automatically set up the environment path correctly.

However, there are cases that the `path` variable and `JAVE_HOME` variable are not being configured properly.

See this [page](https://www.baeldung.com/java-home-vs-path-env-var) for explanation.

In the command prompt, try `java -version` to see what version of Java is first being found in your specific cases. 

Follow the steps below : 

1. Locate the folder with Java installation. For instance, "C:\Program Files\Java\jdk-17" may be your JDK's location.

2. Under System variable, ensure `JAVA_HOME` has been added and set up correct as follows:

> set JAVA_HOME=C:\Program Files\Java\jdk-17

2a. Alternatively, one may start a command prompt and type this `set JAVA_HOME="C:\Program Files\Java\jdk-17"` 

3. Under both User and the System variable, set the `PATH` variable to include the JDK folder. For instance,

> set PATH=C:\Program Files\Java\jdk-17\bin

or 

> set PATH="%JAVA_HOME%\bin";%PATH%

> Note 2 : The order of precedence inside `PATH` variable is crucial. The first available folder having Java
executable inside will be the one to be loaded by Windows OS.

> Note 2a : The `\bin` may be crucial. When running `java -jar xxxx.jar`, mars-sim will look for the
presence of the `java.exe` in Windows OS. If `\bin` is missing in the `JAVA_HOME` variable,
the Windows OS may not be able to locate the `java.exe` and may continue to go down the `PATH`
variable to look for a valid JDK folder. If it's not found, java cannot start mars-sim.

> Note 3 : The BEST approach is to enable only one Java build (such as Java 17.0.8)
inside `PATH` variable and remove all other folders referencing other java versions/builds.

4. Remove any path similar to `C:\ProgramData\Oracle\Java\javapath;`  in `PATH` variable. It can
interfere with the correct version of Java that you would like to use.

> Note 4 : Depending on the order of precedence in `Path` variable,
`C:\ProgramData\Oracle\Java\javapath` can load the undesired version of jre/jdk,
instead of the java version you prefer.

5. To test the version of Java that your machine is using, type "java -version"
in a command prompt window.

6. It's possible for a machine to have multiple versions of Java installed.
To check if a particular Oracle version of Java is being *enabled*,
start [Java Control Panel (JCP)](https://www.java.com/en/download/help/win_controlpanel.html)
from the Control Panel as follows :

* Move your mouse to the magnifier icon (the 2nd icon from the left) on win 10 task bar.
* Type `Configure Java`.
* Hover your mouse over the `Configure Java` and click to start the `Java Control Panel`.
* Click on `Java` tab on top.
* Click on `View` button to open up another panel window.
* Click on the checkbox on the `Enable` column to enable or disable any installed versions of Java.

> Note 5 : In JCP, each row represents a version of Java. Unfortunately, this panel
only tracks the official Oracle versions. If you install any openJDK's on
your machine, JCP won't be able to recognize them.

7. To track what versions of openjdk have been installed on your machine, you may try using 
[JDKMon](https://harmoniccode.blogspot.com/2021/04/friday-fun-lxiii-jdkmon.html).

## Remote Console Connection

To set up true headless mode in your platform, follow the steps in this
[wiki](https://github.com/mars-sim/mars-sim/wiki/Remote-Console-Connection).

## Outreach
Feel free to use our [Facebook community](https://www.facebook.com/groups/125541663548/)
to discuss relevant topics with regard to the development of mars-sim. See also
old/archived [SF discussions](https://sourceforge.net/p/mars-sim/discussion/).

## Discussions
Feel free to start a thread on a particular topic at our GitHub
[Discussion](https://github.com/mars-sim/mars-sim/discussions) page.

## Issues and Tickets
* Current : [GH Issues](https://github.com/mars-sim/mars-sim/issues)
* Past/Archived : [SF Issues](https://sourceforge.net/p/mars-sim/tickets/search/?q=status%3Awont-fix+or+status%3Aclosed)

Help us by filling in the info below when submitting an issue :

**Describe the bug**
 - A clear and concise description of what the bug is.

**Affected Area**
 - What area(s) are we dealing with ? [e.g. Construction, Mission, Resupply,
 Settlement Map, Mini-map, Saving/Loading Sim, System Exceptions in Command
 Prompt/Terminal, etc..]

**Expected behaviors**
 - A clear and concise description of what you expected to happen.

**Actual/Observed Behaviors**
 - A clear and concise description of what you have actually seen.

**Reproduction (optional)**
 - Steps to reproduce the problem

**Screenshots**
 - If applicable, add screenshots to help explain your problem.
e.g. Include the followings :
 - Person Window showing various activity tabs
 - Settlement/Vehicle Window
 - Monitor Tool's showing People/Vehicle/Mission tabs
 - Settlement Map, etc.

**Specifications (please complete)**
 - OS version : [e.g. Windows 10, macOS 10.13, Ubuntu 14.04]
 - Java version : [e.g. Oracle JDK 17.0.8, AdoptOpenJDK 17.0.8, openjfx 17]
 - Major version and build : [e.g. 3.6.0 build 8558]

**Additional context**
 - Add any other context about the problem here. By providing more info above when filing it, 
   you help expedite the handling of the issues you submit.

> Note 1 : if you double-click the jar file to start mars-sim and nothing shows up, 
it's possible that an instance of JVM is already being created in the background 
that has failed to load Main Window. To see if it's indeed the case, in Windows OS, 
you may hit Ctrl+ESC to bring up the Task Manager and scroll down to see any 
*orphaned* instances of `Java(TM) Platform SE binary` running in the background. 
Be sure you first clear them off the memory by right-clicking on it and choosing `End Task`.


### Contribution
We welcome anyone to contribute to mars-sim in terms of ideas, concepts and coding. If you would like to contribute
to coding, see this [wiki](https://github.com/mars-sim/mars-sim/wiki/Development-Environment) for developers.
Also, we will answer your questions in our [Gitter chatroom](https://gitter.im/mokun/mars-sim).


## Website
For a more detail description of this project, see our [project website](https://mars-sim.github.io/).


## Wiki
* Check out our [wikis](https://github.com/mars-sim/mars-sim/wiki) at GitHub.


## Supported Platforms
* Windows
* MacOS
* Linux


## Official Codebase
* https://github.com/mars-sim/mars-sim


## Download
* Check out the most recent release or pre-release build in GitHub [Release](https://github.com/mars-sim/mars-sim/releases) page.

* Or see the previous and current official release versions at
[SourceForge Repo](https://sourceforge.net/projects/mars-sim/files/mars-sim/3.6.0/).

Note: if you prefer, click SF's button below to automatically sense the correct OS platform to download.

[![Download Mars Simulation Project](https://a.fsdn.com/con/app/sf-download-button)](https://sourceforge.net/projects/mars-sim/files/latest/download)


## License
This project is licensed under the terms of the GPL v3.0 license.
