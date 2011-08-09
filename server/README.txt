Discovery service



Configuration File
==================
discovery.events.enabledlist - A comma separated list of DiscoveryEventType enums, or "*" for all, to control which events are published



MBeans
===
Event configuration : A live JMX interface to the "discovery.events.enabledlist" configuration.
	ObjectName : com.proofpoint.discovery.event:name=DiscoveryEventConfig
	ClassName : com.proofpoint.discovery.event.DiscoveryEventConfig
	Attribute : EnabledEvents



Events
======
If an Event Collector is registered in this Discovery server, events will be generated for interesting activities.
Events can be enabled or disabled using both the 'discovery.events.enabledlist' configuration property and the
'com.proofpoint.discovery.event.DiscoveryEventConfig' MBean.

Static Announce : An event for each static announcement posted
	Event type - appservices:type=discovery, name=StaticAnnounce
	Success : 'true' if the announcement is successful, 'false' otherwise
	Duration : Time taken to service the announcement, in double precision milliseconds
	RemoteAddress : Remote request address as seen by the server
	Id : UUID given to the announcement
	Environment : Environment of the announcement
	Location : Location of the announcement
	Type : Type of the announcement
	Pool : Pool of the announcement
	Properties : Map.toString() representation of the announcement properties
	
Static List : An event for each listing of static announcements
	Event type - appservices:type=discovery, name=StaticList
	Success : 'true' if the list is successful, 'false' otherwise
	Duration : Time taken to service the query, in double precision milliseconds
	ResultCount : Number of announcements returned

Static Delete : An event for each static announcement deleted
	Event type - appservices:type=discovery, name=StaticDelete
	Success : 'true' if the deletion is successful, 'false' otherwise
	Duration : Time taken to service the deletion, in double precision milliseconds
	RemoteAddress : Remote request address as seen by the server
	Id : UUID of the deleted announcement
	
Dynamic Announce : An event for each dynamic announcement posted
	Event type - appservices:type=discovery, name=DynamicAnnounce
	Success : 'true' if the announcement is successful, 'false' otherwise
	Duration : Time taken to service the announcement, in double precision milliseconds
	RemoteAddress : Remote request address as seen by the server
	Id : UUID given to the announcement
	Environment : Environment of the announcement
	Location : Location of the announcement
	Type : Type of the announcement
	Pool : Pool of the announcement
	Properties : Map.toString() representation of the announcement properties
	
Dynamic Delete : An event for each dynamic announcement deleted
	Event type - appservices:type=discovery, name=DynamicDelete
	Success : 'true' if the deletion is successful, 'false' otherwise
	Duration : Time taken to service the deletion, in double precision milliseconds
	RemoteAddress : Remote request address as seen by the server
	Id : UUID of the deleted announcement

Dynamic Query : An event posted for each dynamic announcement query
	Event type - appservices:type=discovery, name=ServiceQuery
	Success : 'true' if the list is successful, 'false' otherwise
	Duration : Time taken to service the query, in double precision milliseconds
	Type : Announcement type filter
	Pool : Announcement pool filter
	ResultCount : Number of announcements returned
	
	