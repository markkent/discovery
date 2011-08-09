require 'rubygems'
require 'optparse'
require 'json'

# CLI parser for sdiscovery

class SDiscoveryCommand

  # Map of options that defined the operation.  See command().
  @option_map

  # Map of options that produced JSON - the @static_announcement
  @json_map

  # Array of hosts.  Defaults to 'http://localhost:8080'
  @hosts

  # Service ID when deleting
  @id

  # Static announcement to be JSON encoded
  @static_announcement

  # If help was needed, the message goes here.  See help?().
  @help_message

  @show_parser
  @add_parser
  @delete_parser

  attr_reader :option_map, :json_map, :hosts, :id, :static_announcement, :help_message
  # Returns 'ADD', 'SHOW', 'DELETE', or 'HELP'
  def command ()
    return @option_map[:command]
  end

  # Returns true if help was requested and there's help in help_message
  def help?()
    return @help_message ? true : false
  end

  def initialize (args)
    @option_map = {}
    @json_map = {}

    @show_parser = OptionParser.new do |opts|
      opts.banner = "Usage: #{opts.program_name()} show alias|URL [OUTPUT_OPTION]"

      opts.on('-h', '--help', 'Display this screen') do
        @help_message= help()
      end

      opts.on('--output FMT', '-o', %w{JSON json ID id}, 'Output option: format JSON or ID') do |value|
        @option_map[:output]=value.upcase()
      end
    end

    @add_parser = OptionParser.new do |opts|
      opts.banner = "Usage: #{opts.program_name()} add alias|URL [OUTPUT_OPTION] SERVICE_DEFINITION"

      opts.on('-h', '--help', 'Display this screen') do
        @help_message= help()
      end

      opts.on('--output FMT', '-o', %w{JSON json ID id}, 'Output option: format JSON or ID') do |value|
        @option_map[:output]=value.upcase()
      end

      opts.on('--environment ENVIRONMENT', '-e', "Service definition: environment") do |value|
        @json_map[:environment]=value
      end

      opts.on('--type TYPE', '-t', "Service definition: type") do |value|
        @json_map[:type]=value
      end

      opts.on('--pool POOL', '-p', "Service definition: pool") do |value|
        @json_map[:pool]=value
      end

      opts.on('--location LOCATION', '-l', "Service definition: location") do |value|
        @json_map[:location]=value
      end

      opts.on("-D<key>=<value>", "Sets a service property") do |value|
        value.scan(/(\w+)=(.*)/) do |k,v|
          if @option_map[:properties].nil?
            @option_map[:properties] = {k => v};
          else
            @option_map[:properties][k]=v;
          end
        end
      end

      opts.on('--JSON JSON', '-j', "Service definition: Specify entire raw JSON data") do |value|
        @option_map[:json]=value
      end

      opts.on('--JSONFile JSONFile', '-f', "Service definition: Specify entire raw JSON data filename or '-' for stdin") do |value|
        @option_map[:jsonfile]=value
      end
    end


    @delete_parser = OptionParser.new do |opts|
      opts.banner = "Usage: #{opts.program_name()} delete alias|URL ID"

      opts.on('-h', '--help', 'Display this screen') do
        @help_message= help()
      end
    end


    @option_map[:command]= (args.shift() or "?").upcase()

    case @option_map[:command]
      when "ADD"
        parse_add_command (args)
      when "SHOW"
        parse_show_command (args)
      when "DELETE"
        parse_delete_command (args)
      else
        @help_message= "#{@show_parser}#{@add_parser}#{@delete_parser}\n"
    end
  end

  private

  # Exit for missing argument
  def err_missing_option (message, usage)
    err_missing("argument --#{message}", usage)
  end

  # Exit for missing field
  def err_missing (message, usage)
    raise "Missing #{message}\n#{usage}"
  end

  # Exit for extra k/v arguments
  def err_extra_kv (args, usage)
    message= "Extra options:\n"
    args.each() {|k,v| message = message + "   --#{k}=#{v}\n"}
    message = message + usage.to_s()
    raise message
  end

  # Exit for extra whole arguments
  def err_extra_arg (args, usage)
    message= "Extra options:\n"
    args.each() {|v| message = message + "   #{v}\n"}
    message = message + usage.to_s()
    raise message
  end

  # Shift off one argument and parse it as an alias in .discoveryrc or a hostname.
  # Default is localhost entry in .disoveryrc or http://localhost:8080
  # Set result in @hosts array
  def parse_hosts (args)
    host_arg = (args.empty? || (args[0][0,1] == '-')) ? 'localhost' : args.shift()
    discoveryrc = File.expand_path("~/.discoveryrc")
    aliasmap = {}
    if File.readable?(discoveryrc)
      File.readlines(discoveryrc).each {|line| line.scan(/(\w+)\s*=\s*(.*)/) {|k,v| aliasmap[k]=v}}
    end

    host_list = ''
    if aliasmap.has_key?(host_arg)
      host_list = aliasmap[host_arg]
    elsif host_arg == 'localhost'
      host_list = 'http://localhost:8080'
    else
      host_list = host_arg
    end

    @hosts = host_list.split(',').map() {|host| host.strip()};
    return @hosts
  end

  def parse_add_command (args)
    #Pull host
    parse_hosts(args)

    # Pull dashed options
    begin
      @add_parser.parse!(args)
    rescue OptionParser::ParseError => err
      raise "#{err}\n#{@add_parser}"
    end

    #Check argument list is now empty
    if !args.empty?
      err_extra_arg(args, @add_parser)
    end

    # Check conflict - StaticAnnouncement options with JSON input
    if (@option_map[:jsonfile] || @option_map[:json]) && !@json_map.empty?
      err_extra_kv(@json_map, @add_parser)
    end

    # Set output from options
    output = (@option_map[:output] == 'ID') ? :id : :json

    # Build StaticAnnouncement object from options
    if @option_map[:jsonfile]
      @static_announcement = JSON.parse(IO.read(File.expand_path(@option_map[:jsonfile])))
    elsif @option_map[:json]
      if '-' == @option_map[:json]
        @static_announcement = JSON.parse($stdin.read)
      else
        @static_announcement = JSON.parse(@option_map[:json])
      end
    else
      @static_announcement= {}
      @static_announcement['environment'] = @json_map[:environment] or err_missing_option(:environment, @add_parser)
      @static_announcement['type'] = @json_map[:type] or err_missing_option(:type, @add_parser)
      @static_announcement['pool'] = @json_map[:pool] or err_missing_option(:pool, @add_parser)
      @static_announcement['location'] = @json_map[:location]
      @static_announcement['properties']= @json_map[:properties]
    end

  end

  def parse_show_command (args)
    parse_hosts(args)

    begin
      @show_parser.parse!(args)
    rescue OptionParser::ParseError => err
      raise "#{err}\n#{@show_parser}"
    end

    if !args.empty?
      err_extra_arg(args, @show_parser)
    end
  end

  def parse_delete_command (args)
    @id = args.pop() or err_missing("service identifier", @delete_parser)
    parse_hosts(args)

    begin
      @delete_parser.parse!(args)
    rescue OptionParser::ParseError => err
      raise "#{err}\n#{@delete_parser}"
    end

  end
end
