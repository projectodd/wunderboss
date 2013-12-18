module WunderBoss
  class << self
    attr_reader :container
        
    def container(opts = nil)
      if opts
        Java::OrgProjectoddWunderboss::WunderBoss.new(opts)
      else
        @container = Java::OrgProjectoddWunderboss::WunderBoss.new unless @container
        @container
      end
    end
  end
end
