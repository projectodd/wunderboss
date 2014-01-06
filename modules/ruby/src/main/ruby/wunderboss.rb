module WunderBoss
  class << self
        
    def container(opts = nil)
      if opts
        Java::OrgProjectoddWunderboss::WunderBoss.new(opts)
      else
        Java::OrgProjectoddWunderboss::WunderBoss.new
      end
    end
  end
end
