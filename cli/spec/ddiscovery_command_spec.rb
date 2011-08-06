require 'rubygems'
require 'json'
require 'rspec'
require 'DDiscoveryCommand'

describe 'Dynamic Discovery command option parsing' do

  it 'should know if the caller asked for help with a short option' do
    parser = DDiscoveryCommand.new(["-h"])
    parser.help?.should == true
  end

  it 'should know if the caller asked for help with a long option' do
    parser = DDiscoveryCommand.new(["--help"])
    parser.help?.should == true
  end

  it 'should fail if an unknown option is specified' do
    lambda { DDiscoveryCommand.new(["--this_is_an_unknown_option"])}.should raise_exception(RuntimeError, /invalid option: --this_is_an_unknown_option/)
  end

  it 'should read aliases out of the .discoveryrc file' do

    File.should_receive(:expand_path).with("~/.discoveryrc").and_return("/path/to/.discoveryrc")
    File.should_receive(:readable?).with("/path/to/.discoveryrc").and_return(true)
    File.should_receive(:readlines).with("/path/to/.discoveryrc").and_return(
        ["alias1=http://discovery:8080", "alias2=http://discovery:8081,http://discovery:8082"])

    parser = DDiscoveryCommand.new(["alias2"])
    parser.hosts.should == ["http://discovery:8081", "http://discovery:8082"]

  end

  describe 'when type and pool are not specified' do

    before(:each) do
      @args = [ "http://another.host:8080" ]
    end

    it 'should construct with a default hostname' do
        parser = DDiscoveryCommand.new([])
        parser.hosts.should == ["http://localhost:8080"]
    end

    it 'should construct and allow the specified host to be read' do
      parser = DDiscoveryCommand.new(@args)
      parser.hosts.should == ["http://another.host:8080"]
    end

  end

  describe 'when type is specified' do

    before(:each) do
      @args = [ "http://another.host:8080", "--type", "user" ]
    end

    it 'should construct when the host is specified and the service is identified by --type argument' do
      parser = DDiscoveryCommand.new(@args)
      parser.type == "user"
      parser.hosts.should == ["http://another.host:8080"]
    end

    it 'should construct when the host is not specified and the service is identified by --type argument' do
      parser = DDiscoveryCommand.new(["--type", "user"])
      parser.type == "user"
      parser.hosts.should == ["http://localhost:8080"]
    end
  end

  describe 'when pool is specified' do
    before(:each) do
      @args = [ "http://another.host:8080", "--pool", "general" ]
    end

    it 'should construct when the pool is identified by --pool argument' do
      parser = DDiscoveryCommand.new(@args)
      parser.pool == "general"
      parser.hosts.should == ["http://another.host:8080"]
    end

    it 'should construct when the host is not specified and the pool is identified by --pool argument' do
      parser = DDiscoveryCommand.new(["--pool", "general"])
      parser.pool == "general"
      parser.hosts.should == ["http://localhost:8080"]
    end

  end

  describe 'when type and pool are specified' do
    before(:each) do
      @args = [ "http://another.host:8080", "--type", "user", "--pool", "general"]
    end

    it 'should construct when the service is identified by --type and --pool arguments' do
      parser = DDiscoveryCommand.new(@args)
      parser.type == "user"
      parser.pool == "general"
      parser.hosts.should == ["http://another.host:8080"]
    end

    it 'should construct when the host is not specified and the type and pool arguments are specified' do
      parser = DDiscoveryCommand.new(["--type", "user", "--pool", "general"])
      parser.pool == "general"
      parser.type == "user"
      parser.hosts.should == ["http://localhost:8080"]
    end

  end

end
